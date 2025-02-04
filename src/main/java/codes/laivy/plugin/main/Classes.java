package codes.laivy.plugin.main;

import codes.laivy.plugin.annotation.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

final class Classes {

    // Static initializers

    public static @NotNull Class<?> define(byte[] bytes) {
        try {
            @NotNull Method define = ClassLoader.class.getDeclaredMethod("defineClass", byte[].class, int.class, int.class);
            define.setAccessible(true);
            return (Class<?>) define.invoke(Plugins.class.getClassLoader(), bytes, 0, bytes.length);
        } catch (@NotNull NoSuchMethodException e) {
            throw new RuntimeException("cannot find ClassLoader's #defineClass(byte[], int, int) method", e);
        } catch (@NotNull IllegalAccessException e) {
            throw new RuntimeException("cannot access ClassLoader's #defineClass(byte[], int, int) method", e);
        } catch (@NotNull InvocationTargetException e) {
            throw new RuntimeException("cannot invoke ClassLoader's #defineClass(byte[], int, int) method", e);
        }
    }
    public static byte[] toByteArray(@NotNull InputStream input) throws IOException {
        @NotNull ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[2048];

        int bytesRead;
        while ((bytesRead = input.read(data)) != -1) {
            buffer.write(data, 0, bytesRead);
        }

        return buffer.toByteArray();
    }
    public static @NotNull Map<@NotNull String, @NotNull InputStream> getAllPluginTypeClasses() throws IOException {
        @NotNull Map<String, InputStream> classes = new HashMap<>();
        @NotNull String home = System.getProperty("java.home");

        for (@NotNull String path : System.getProperty("java.class.path").split(File.pathSeparator)) {
            if (path.startsWith(home)) {
                continue;
            }

            @NotNull File file = new File(path);

            if (file.isDirectory()) {
                findClassesInDirectory(file, "", classes);
            } else if (file.getName().endsWith(".jar")) {
                findClassesInJar(file, classes);
            }
        }
        return classes;
    }
    public static @NotNull Map<@NotNull String, @NotNull URL> read(@NotNull ClassLoader loader, @NotNull URL url) throws IOException {
        @NotNull Map<@NotNull String, @NotNull URL> map = new LinkedHashMap<>();
        @NotNull String protocol = url.getProtocol();

        if ("file".equals(protocol)) {
            @NotNull Map<String, URL> temp = new LinkedHashMap<>();
            @NotNull File directory = new File(URLDecoder.decode(url.getPath(), "UTF-8"));

            if (directory.isDirectory()) listFiles(directory, temp);

            for (@NotNull Entry<@NotNull String, @NotNull URL> entry : temp.entrySet()) try {
                map.put(entry.getKey().substring(url.toURI().getPath().length() - 1), entry.getValue());
            } catch (@NotNull URISyntaxException e) {
                throw new RuntimeException("cannot generate uri from url: " + url, e);
            }
        }

        return map;
    }

    // Private utilities

    private static boolean hasPluginType(byte[] bytecode) {
        @NotNull String interfaceName = Plugin.class.getName().replace('.', '/');
        @NotNull AtomicBoolean bool = new AtomicBoolean(false);

        @NotNull ClassReader classReader = new ClassReader(bytecode);
        classReader.accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                if (descriptor.contains(interfaceName)) {
                    bool.set(true);
                }

                return super.visitAnnotation(descriptor, visible);
            }
        }, 0);

        return bool.get();
    }
    private static void findClassesInDirectory(@NotNull File directory, @NotNull String packageName, @NotNull Map<@NotNull String, @NotNull InputStream> classes) throws IOException {
        // Retrieve directory files
        @NotNull File[] files = directory.listFiles();
        if (files == null) files = new File[0];

        // Read all files
        for (@NotNull File file : files) {
            if (file.isDirectory()) {
                findClassesInDirectory(file, packageName + file.getName() + ".", classes);
            } else if (file.getName().endsWith(".class")) {
                byte[] bytecode = toByteArray(Files.newInputStream(file.toPath()));

                if (hasPluginType(bytecode)) {
                    @NotNull String name = packageName + file.getName().replace(".class", "");
                    classes.put(name, Files.newInputStream(file.toPath()));
                }
            }
        }
    }
    private static void findClassesInJar(@NotNull File file, @NotNull Map<@NotNull String, @NotNull InputStream> classes) throws IOException {
        try (@NotNull JarFile jar = new JarFile(file)) {
            @NotNull Enumeration<JarEntry> entries = jar.entries();

            while (entries.hasMoreElements()) {
                @NotNull JarEntry entry = entries.nextElement();

                if (entry.getName().endsWith(".class")) {
                    byte[] bytecode = toByteArray(jar.getInputStream(entry));

                    if (hasPluginType(bytecode)) {
                        @NotNull String name = entry.getName().replace("/", ".").replace(".class", "");
                        classes.put(name, new ByteArrayInputStream(bytecode));
                    }
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
