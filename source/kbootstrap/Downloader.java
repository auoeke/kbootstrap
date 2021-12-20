package kbootstrap;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilderFactory;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.impl.launch.FabricLauncherBase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Node;

public class Downloader {
    private static final Logger logger = LogManager.getLogger("KBootstrap");
    private static final HttpClient http = HttpClient.newHttpClient();

    public static void download() {
        try {
            downloadKotlin(false, "stdlib");
            downloadKotlin(true, "serialization-core-jvm");
            downloadKotlin(true, "serialization-json-jvm");

            var files = Downloader.class.getClassLoader().resources("kbootstrap-modules").toList();
            var modules = new HashSet<String>();

            for (var file : files) {
                Collections.addAll(modules, new String(file.openStream().readAllBytes()).split(":"));
            }

            modules.forEach(it -> {
                switch (it.toLowerCase(Locale.ROOT)) {
                    case "coroutines" -> Stream.of("coroutines-core", "coroutines-core-jvm", "coroutines-jdk8", "coroutines-jdk9").forEach(library -> downloadKotlin(true, library));
                    case "reflect" -> downloadKotlin(false, "reflect");
                }
            });
        } catch (Throwable throwable) {
            throw rethrow(throwable);
        }
    }

    private static void downloadKotlin(boolean x, String name) {
        try {
            var base = x ? "kotlinx" : "kotlin";
            var root = "https://repo.maven.apache.org/maven2/org/jetbrains/%s/%1$s-%s/".formatted(base, name);
            var version = childNode(childNode(DocumentBuilderFactory.newDefaultInstance().newDocumentBuilder().parse(root + "maven-metadata.xml").getFirstChild(), "versioning"), "release").getTextContent();
            root += version + '/';
            var filename = "%s-%s-%s.jar".formatted(base, name, version);
            download(root + filename, Files.createDirectories(FabricLoader.getInstance().getConfigDir().resolve("kbootstrap")).resolve(filename));
        } catch (Throwable exception) {
            throw rethrow(exception);
        }
    }

    private static void download(String url, Path destination) {
        try {
            if (!Files.exists(destination)) {
                logger.info("Downloading {} ({}) as {}.", url, readableSize(http.send(
                    HttpRequest.newBuilder().uri(new URI(url)).method("HEAD", HttpRequest.BodyPublishers.noBody()).build(),
                    HttpResponse.BodyHandlers.discarding()
                ).headers().firstValueAsLong("content-length").orElseThrow()), destination);

                http.send(
                    HttpRequest.newBuilder().uri(new URI(url)).build(),
                    HttpResponse.BodyHandlers.ofFile(destination, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)
                );
            }

            FabricLauncherBase.getLauncher().addToClassPath(destination);
        } catch (Throwable throwable) {
            throw rethrow(throwable);
        }
    }

    private static Node childNode(Node node, String name) {
        var children = node.getChildNodes();

        for (int index = 0, length = children.getLength(); index < length; index++) {
            var child = children.item(index);

            if (child.getNodeName().equals(name)) {
                return child;
            }
        }

        return null;
    }

    private static String readableSize(long bytes) {
        var units = new long[]{1 << 30, 1 << 20, 1 << 10, 1};
        var names = new String[]{"GiB", "MiB", "KiB", "B"};
        var newValue = 0D;
        int index;

        for (index = 0; index < units.length; index++) {
            var unit = units[index];

            if (bytes > unit) {
                newValue = (double) bytes / unit;

                break;
            }

            if (bytes == unit) {
                newValue = 1;

                break;
            }
        }

        var string = String.valueOf(newValue);
        return String.join(" ", newValue > 100 ? string.substring(0, 3) : string.substring(0, Math.min(4, string.length())), names[index]);
    }

    private static <T extends Throwable> RuntimeException rethrow(Throwable throwable) throws T {
        throw (T) throwable;
    }
}
