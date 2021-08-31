package net.auoeke.kbootstrap;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandleProxies;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
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
import java.util.function.BooleanSupplier;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilderFactory;
import kotlin.Metadata;
import net.fabricmc.loader.api.LanguageAdapter;
import net.fabricmc.loader.api.LanguageAdapterException;
import net.fabricmc.loader.api.ModContainer;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Node;

@SuppressWarnings("unchecked")
public class KBootstrap implements LanguageAdapter {
    @SuppressWarnings("UnusedAssignment") // what
    private static transient HttpClient http = HttpClient.newHttpClient();
    private static transient Logger logger = LogManager.getLogger("KBootstrap");
    private static transient ClassLoader knotLoader = KBootstrap.class.getClassLoader();
    private static transient MethodHandle addURL;

    public static boolean download(boolean x, String name, String typeName) {
        var downloaded = false;

        try {
            var base = x ? "kotlinx" : "kotlin";
            var root = "https://repo.maven.apache.org/maven2/org/jetbrains/%s/%1$s-%s/".formatted(base, name);
            var version = childNode(childNode(DocumentBuilderFactory.newDefaultInstance().newDocumentBuilder().parse(root + "maven-metadata.xml").getFirstChild(), "versioning"), "release").getTextContent();
            root += version + '/';

            var kotlinVersion = typeName == null ? null : libraryVersion(typeName);

            if (kotlinVersion == null || compareVersions(kotlinVersion, parseVersion(version)) == 1) {
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

    public static boolean download(boolean x, String name) {
        return download(x, name, null);
    }

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

    private static Field companionField(Class<?> companionType) {
        var declaringType = companionType.getDeclaringClass();

        if (declaringType != null) {
            for (var field : declaringType.getDeclaredFields()) {
                var staticFinal = Modifier.STATIC | Modifier.FINAL;

                if (field.getType() == companionType && (field.getModifiers() & staticFinal) == staticFinal) {
                    return field;
                }
            }
        }

        return null;
    }

    private static <T> T findInstance(Class<?> type) throws ReflectiveOperationException {
        Field field;

        try {
            field = type.getDeclaredField("INSTANCE");
        } catch (NoSuchFieldException exception) {
            field = companionField(type);
        }

        if (field == null) {
            return null;
        }

        field.trySetAccessible();

        return (T) field.get(null);
    }

    private static Field findField(Class<?> target, String name) throws NoSuchFieldException {
        Field field;

        getField:
        try {
            field = target.getDeclaredField(name);
        } catch (NoSuchFieldException exception1) {
            var staticFinal = Modifier.STATIC | Modifier.FINAL;

            if ((target.getModifiers() & staticFinal) == staticFinal) {
                try {
                    field = target.getDeclaringClass().getDeclaredField(name);

                    if ((field.getModifiers() & staticFinal) == staticFinal) {
                        break getField;
                    }
                } catch (NoSuchFieldException ignored) {}
            }

            throw exception1;
        }

        var modifiers = field.getModifiers();

        if ((modifiers & Modifier.FINAL) == 0) {
            throw new NoSuchFieldException("Field \"%s\" is not final.".formatted(name));
        }

        field.trySetAccessible();

        return field;
    }

    private static <T> T instantiate(Class<?> type) throws ReflectiveOperationException {
        T instance = findInstance(type);

        if (instance != null) {
            return instance;
        }

        var constructor = type.getDeclaredConstructor();
        constructor.trySetAccessible();

        return (T) constructor.newInstance();
    }

    private static <T> T create(String value, Class<T> type) {
        try {
            var components = value.split("::");

            if (components.length > 2) {
                components = new String[]{Stream.of(components).limit(components.length - 1).collect(Collectors.joining("$")), components[components.length - 1]};
            }

            var target = Class.forName(components[0]);

            if (target.getAnnotation(Metadata.class) == null) {
                throw new LanguageAdapterException(target.getName() + " is not a Kotlin class.");
            }

            if (components.length == 1) {
                return instantiate(target);
            }

            try {
                var method = target.getDeclaredMethod(components[1]);
                var handle = MethodHandles.privateLookupIn(target, MethodHandles.lookup()).unreflect(method);

                if (!Modifier.isStatic(method.getModifiers())) {
                    handle = handle.bindTo(instantiate(target));
                }

                return MethodHandleProxies.asInterfaceInstance(type, handle);
            } catch (NoSuchMethodException exception) {
                var field = findField(target, components[1]);

                return (T) field.get(Modifier.isStatic(field.getModifiers()) ? null : instantiate(target));

            }
        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }

    @Override
    public <T> T create(ModContainer mod, String value, Class<T> type) {
        return create(value, type);
    }

    static {
        try {
            addURL = MethodHandles.privateLookupIn(knotLoader.getClass(), MethodHandles.lookup()).bind(knotLoader, "addURL", MethodType.methodType(void.class, URL.class));
        } catch (ReflectiveOperationException exception) {
            throw new RuntimeException(exception);
        }

        if (download(false, "stdlib", "kotlin.Unit") | Stream.of("Coroutines", "Reflect", "Serialization").map(module -> ((BooleanSupplier) create("net.auoeke.kbootstrap." + module, null)).getAsBoolean()).reduce(false, (first, second) -> first | second)) {
            logger.info("Done.");
        }

        http = null;
        logger = null;
        knotLoader = null;
        addURL = null;
    }
}
