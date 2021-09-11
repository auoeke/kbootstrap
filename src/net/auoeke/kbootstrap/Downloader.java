package net.auoeke.kbootstrap;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import javax.xml.parsers.DocumentBuilderFactory;
import net.auoeke.safe.Safe;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Node;

class Downloader {
    static final Logger logger = LogManager.getLogger("KBootstrap");

    private static final HttpClient http = HttpClient.newHttpClient();
    private static final MethodHandle addURL;

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
        var units = new long[]{1024 * 1024, 1024, 1};
        var names = new String[]{"MiB", "KiB", "B"};
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

        return (newValue > 100 ? string.substring(0, 3) : string.substring(0, Math.min(4, string.length()))) + " " + names[index];

    }

    static void download(boolean x, String name) {
        try {
            var base = x ? "kotlinx" : "kotlin";
            var root = "https://repo.maven.apache.org/maven2/org/jetbrains/%s/%1$s-%s/".formatted(base, name);
            var version = childNode(childNode(DocumentBuilderFactory.newDefaultInstance().newDocumentBuilder().parse(root + "maven-metadata.xml").getFirstChild(), "versioning"), "release").getTextContent();
            root += version + '/';

            var destination = Files.createDirectories(Path.of(System.getProperty("java.io.tmpdir"), "net.auoeke/kbootstrap"));
            var filename = "%s-%s-%s.jar".formatted(base, name, version);
            var jar = destination.resolve(filename);

            if (!Files.exists(jar)) {
                var url = "%s%s".formatted(root, filename);

                logger.info("Downloading {} ({}) into {}.", url, readableSize(http.send(
                    HttpRequest.newBuilder().uri(new URI(url)).method("HEAD", HttpRequest.BodyPublishers.noBody()).build(),
                    HttpResponse.BodyHandlers.discarding()
                ).headers().firstValueAsLong("content-length").orElseThrow()), jar);

                http.send(
                    HttpRequest.newBuilder().uri(new URI(url)).build(),
                    HttpResponse.BodyHandlers.ofFile(jar, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)
                );
            }

            addURL.invoke(jar.toString());
        } catch (Throwable exception) {
            throw new RuntimeException(exception);
        }
    }

    static {
        try {
            addURL = Safe.lookup.bind(ClassLoader.getSystemClassLoader(), "appendClassPath", MethodType.methodType(void.class, String.class));
        } catch (ReflectiveOperationException exception) {
            throw new RuntimeException(exception);
        }
    }
}
