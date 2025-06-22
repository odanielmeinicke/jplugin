package dev.meinicke.plugin.main;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReader;
import java.nio.file.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Utility to discover all available classes in the current JVM runtime,
 * including classes in classpath entries (jars and directories) and modules.
 * Does not use instrumentation.
 */
final class Classes {

    // Static initializers

    /**
     * Scans the classpath and modules for all .class files and returns their
     * fully-qualified names (without loading the classes).
     */
    public static void consumeAllClasses(@NotNull Consumer<@NotNull ClassData> consumer) throws IOException {
        // 1. Scan traditional classpath
        @NotNull String cp = System.getProperty("java.class.path", "");

        if (!cp.isEmpty()) {
            @NotNull String[] entries = cp.split(File.pathSeparator);

            for (String entry : entries) {
                @NotNull Path path = Paths.get(entry);

                try {
                    if (Files.isDirectory(path)) {
                        scanDirectory(path, consumer);
                    } else if (entry.toLowerCase().endsWith(".jar") && Files.exists(path)) {
                        scanJar(path, consumer);
                    }
                } catch (IOException e) {
                    System.err.println("[Classes] Failed to scan entry: " + entry + "; " + e.getMessage());
                }
            }
        }

        // 2. Scan modules (Java 9+)
        ModuleFinder.ofSystem().findAll().forEach(ref -> {
            try (@NotNull ModuleReader reader = ref.open()) {
                reader.list().forEach(resource -> {
                    if (resource.endsWith(".class")) {
                        // Variables
                        @NotNull String name = resource.replace('/', '.').substring(0, resource.length() - 6);

                        // Start reading
                        try (@Nullable InputStream stream = reader.open(name).orElse(null)) {
                            if (stream != null) {
                                consumer.accept(new ClassData(name, stream));
                            }
                        } catch (IOException ignore) {
                        }
                    }
                });
            } catch (IOException e) {
                // skip unreadable modules
            }
        });
    }

    private static void scanDirectory(@NotNull Path root, @NotNull Consumer<@NotNull ClassData> consumer) throws IOException {
        try (@NotNull Stream<Path> stream = Files.walk(root)) {
            stream.filter(p -> p.toString().endsWith(".class"))
                    .forEach(path -> {
                        try (@NotNull InputStream input = Files.newInputStream(path)) {
                            @NotNull String name = toClassName(root, path);
                            consumer.accept(new ClassData(name, input));
                        } catch (IOException ignore) {
                        }
                    });
        }
    }

    private static void scanJar(@NotNull Path jarPath, @NotNull Consumer<@NotNull ClassData> consumer) throws IOException {
        try (@NotNull FileSystem fs = FileSystems.newFileSystem(jarPath, null)) {
            for (@NotNull Path root : fs.getRootDirectories()) {
                scanDirectory(root, consumer);
            }
        }
    }

    private static @NotNull String toClassName(@NotNull Path root, @NotNull Path classFile) {
        // Variables
        @NotNull Path rel = root.relativize(classFile);
        @NotNull String s = rel.toString().replace(File.separatorChar, '.').replace("/", ".");

        // Finish
        return s.substring(0, s.length() - 6);
    }

    // Object

    private Classes() {
        throw new UnsupportedOperationException("this class cannot be instantiated");
    }

}
