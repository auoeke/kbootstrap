package net.auoeke.kbootstrap;

import java.io.IOException;
import java.io.UncheckedIOException;
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
import java.util.Arrays;
import java.util.Objects;
import java.util.jar.Manifest;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilderFactory;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.LanguageAdapter;
import net.fabricmc.loader.api.ModContainer;
import org.apache.logging.log4j.LogManager;
import org.w3c.dom.Node;

public class KBootstrap implements LanguageAdapter {
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

    private static void download() {
        try {
            var root = "https://repo.maven.apache.org/maven2/org/jetbrains/kotlin/kotlin-stdlib/";
            var version = childNode(childNode(DocumentBuilderFactory.newDefaultInstance().newDocumentBuilder().parse(root + "maven-metadata.xml").getFirstChild(), "versioning"), "release").getTextContent();
            root += version + '/';

            var destination = Files.createDirectories(Path.of(System.getProperty("java.io.tmpdir"), "net.auoeke.kbootstrap"));
            var filename = "kotlin-stdlib-%s.jar".formatted(version);
            var jar = destination.resolve(filename);

            if (!Files.exists(jar)) {
                var logger = LogManager.getLogger("KBootstrap");
                var url = "%s%s".formatted(root, filename);
                logger.info("Downloading {} into {}.", url, jar);

                HttpClient.newHttpClient().send(
                    HttpRequest.newBuilder().uri(new URI(url)).build(),
                    HttpResponse.BodyHandlers.ofFile(jar, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)
                );

                logger.info("Done.");
            }

            var knotLoader = KBootstrap.class.getClassLoader();
            MethodHandles.privateLookupIn(knotLoader.getClass(), MethodHandles.lookup()).bind(knotLoader, "addURL", MethodType.methodType(void.class, URL.class)).invoke(jar.toUri().toURL());
        } catch (Throwable exception) {
            throw new RuntimeException(exception);
        }
    }

    @Override
    public <T> T create(ModContainer mod, String value, Class<T> type) {
        return null;
    }

    static {
        try {
            var kotlinVersion = new Manifest(Files.newInputStream(FileSystems.newFileSystem(Path.of(Class.forName("kotlin.Unit").getProtectionDomain().getCodeSource().getLocation().toURI())).getPath("META-INF", "MANIFEST.MF"))).getMainAttributes().getValue("Implementation-Version");

            FabricLoader.getInstance().getAllMods().stream()
                .map(mod -> mod.getMetadata().getCustomValue("kbootstrap:version"))
                .filter(Objects::nonNull)
                .map(version -> {
                    var array = parseVersion(version.getAsString());

                    if (array.length < 2 || array.length > 3) {
                        throw new IllegalArgumentException("Kotlin version (%s) may contain either 2 or 3 components.".formatted(version));
                    }

                    return array.length < 3 ? Arrays.copyOf(array, array.length + 1) : array;
                })
                .reduce((first, second) -> compareVersions(first, second) == -1 ? second : first)
                .ifPresentOrElse(minimumVersion -> {
                    if (compareVersions(minimumVersion, parseVersion(kotlinVersion.substring(0, kotlinVersion.indexOf('-')))) == 1) {
                        download();
                    }
                }, KBootstrap::download);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        } catch (URISyntaxException exception) {
            throw new RuntimeException(exception);
        } catch (ClassNotFoundException exception) {
            download();
        }
    }
}
