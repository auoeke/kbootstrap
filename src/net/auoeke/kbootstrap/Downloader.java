package net.auoeke.kbootstrap;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.jar.Manifest;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Node;

class Downloader {
    static final Logger logger = LogManager.getLogger("KBootstrap");

    private static final HttpClient http = HttpClient.newHttpClient();
    private static final ClassLoader knotLoader = Downloader.class.getClassLoader();
    private static final MethodHandle addURL;

    private static int[] libraryVersion(String typeName) {
        try {
            var kotlinVersion = new Manifest(Files.newInputStream(FileSystems.newFileSystem(Path.of(Class.forName(typeName, false, knotLoader).getProtectionDomain().getCodeSource().getLocation().toURI())).getPath("META-INF", "MANIFEST.MF"))).getMainAttributes().getValue("Implementation-Version");

            return parseVersion(StringUtils.substringBefore(kotlinVersion, "-"));
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        } catch (URISyntaxException exception) {
            throw new RuntimeException(exception);
        } catch (ClassNotFoundException ignored) {}

        return null;
    }

    private static int[] parseVersion(String version) {
        return Stream.of(version.split("\\.")).mapToInt(Integer::parseUnsignedInt).toArray();
    }

    private static int compareVersions(int[] first, int[] second) {
        for (int index = 0; index < first.length; index++) {
            if (first[index] > second[index]) {
                return 1;
            }

            if (first[index] < second[index]) {
                return -1;
            }
        }

        return 0;
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

    static boolean download(boolean x, String name, String typeName) {
        var downloaded = false;

        try {
            var base = x ? "kotlinx" : "kotlin";
            var root = "https://repo.maven.apache.org/maven2/org/jetbrains/%s/%1$s-%s/".formatted(base, name);
            var version = childNode(childNode(DocumentBuilderFactory.newDefaultInstance().newDocumentBuilder().parse(root + "maven-metadata.xml").getFirstChild(), "versioning"), "release").getTextContent();
            root += version + '/';

            var kotlinVersion = typeName == null ? null : libraryVersion(typeName);

            if (kotlinVersion == null || compareVersions(parseVersion(version), kotlinVersion) == 1) {
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

                    downloaded = true;
                }

                addURL.invoke(jar.toUri().toURL());
            }
        } catch (Throwable exception) {
            throw new RuntimeException(exception);
        }

        return downloaded;
    }

    static boolean download(boolean x, String name) {
        return download(x, name, null);
    }

    static {
        try {
            addURL = MethodHandles.privateLookupIn(knotLoader.getClass(), MethodHandles.lookup()).bind(knotLoader, "addURL", MethodType.methodType(void.class, URL.class));
        } catch (ReflectiveOperationException exception) {
            throw new RuntimeException(exception);
        }
    }
}
