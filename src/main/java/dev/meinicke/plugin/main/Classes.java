package dev.meinicke.plugin.main;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

final class Classes {

    // Static initializers

    public static void getAllTypeClassesWithVisitor(@NotNull URL url, @NotNull ClassLoader loader, @NotNull Consumer<@NotNull ClassData> consumer) throws IOException {
        // Classes from URL
        if (url.getProtocol().equals("jar")) {
            JarURLConnection jarConnection = (JarURLConnection) url.openConnection();
            findClassesInJar(jarConnection.getJarFile(), loader, consumer);
        } else if (url.getProtocol().equals("file")) {
            // Retrieve file
            @NotNull File file;

            try {
                file = new File(url.toURI());
            } catch (URISyntaxException e) {
                file = new File(url.getPath());
            }

            // List classes
            if (file.isDirectory()) {
                findClassesInDirectory(file, "", loader, consumer);
            } else if (file.getName().endsWith(".jar")) {
                try (JarFile jar = new JarFile(file)) {
                    findClassesInJar(jar, loader, consumer);
                }
            }
        }
    }
    public static void getAllTypeClassesWithVisitor(@NotNull Set<ClassLoader> classLoaders, @NotNull Consumer<@NotNull ClassData> consumer) throws IOException {
        @NotNull String home = System.getProperty("java.home");

        // Classes from ClassLoader
        for (@NotNull ClassLoader classLoader : classLoaders) {
            @NotNull Set<File> files = new HashSet<>();
            @NotNull Enumeration<URL> enumeration = classLoader.getResources("");

            while (enumeration.hasMoreElements()) {
                @NotNull URL url = enumeration.nextElement();

                try {
                    files.add(new File(url.toURI()));
                } catch (@NotNull URISyntaxException | @NotNull IllegalArgumentException e) {
                    files.add(new File(url.getPath()));
                }
            }

            // Load all classes
            for (@NotNull File file : files) {
                @NotNull String path = file.getPath();

                if (path.startsWith(home)) {
                    continue;
                }

                if (file.isDirectory()) {
                    findClassesInDirectory(file, "", classLoader, consumer);
                } else if (file.getName().endsWith(".jar")) {
                    try (@NotNull JarFile jar = new JarFile(file)) {
                        findClassesInJar(jar, classLoader, consumer);
                    }
                }
            }
        }
    }

    // Private utilities

    private static void findClassesInDirectory(@NotNull File directory, @NotNull String packageName, @NotNull ClassLoader loader, @NotNull Consumer<@NotNull ClassData> consumer) throws IOException {
        // Retrieve directory files
        @NotNull File[] files = directory.listFiles();
        if (files == null) files = new File[0];
        
        // Read all files
        for (@NotNull File file : files) {
            if (file.isDirectory()) {
                findClassesInDirectory(file, packageName + file.getName() + ".", loader, consumer);
            } else if (file.getName().endsWith(".class") && !file.getName().toLowerCase().endsWith("module-info.class")) {
                @NotNull String name = packageName + file.getName().replace(".class", "");

                try (@NotNull InputStream stream = Files.newInputStream(file.toPath())) {
                    consumer.accept(new ClassData(name, loader, stream));
                }
            }
        }
    }
    private static void findClassesInJar(@NotNull JarFile jar, @NotNull ClassLoader loader, @NotNull Consumer<@NotNull ClassData> consumer) throws IOException {
        @NotNull Enumeration<JarEntry> entries = jar.entries();

        while (entries.hasMoreElements()) {
            @NotNull JarEntry entry = entries.nextElement();

            if (entry.getName().endsWith(".class") && !entry.getName().toLowerCase().endsWith("module-info.class")) {
                @NotNull String name = entry.getName().replace("/", ".").replace(".class", "");

                try (@NotNull InputStream stream = jar.getInputStream(entry)) {
                    consumer.accept(new ClassData(name, loader, stream));
                }
            }
        }
    }
    private static void listFiles(@NotNull File directory, @NotNull Map<@NotNull String, @NotNull URL> map) throws IOException {
        @NotNull File @Nullable [] files = directory.listFiles();

        if (files != null) {
            for (@NotNull File file : files) {
                if (file.isDirectory()) {
                    listFiles(file, map);
                } else if (file.getName().endsWith(".class")) {
                    map.put(file.getPath(), file.toURI().toURL());
                }
            }
        }
    }

    // Object

    private Classes() {
        throw new UnsupportedOperationException("this class cannot be instantiated");
    }

}
