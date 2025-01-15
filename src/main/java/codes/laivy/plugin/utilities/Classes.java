package codes.laivy.plugin.utilities;

import codes.laivy.plugin.annotation.Plugin;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

import java.io.*;
import java.nio.file.Files;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class Classes {

    // Static initializers

    public static boolean hasPluginType(byte[] bytecode) {
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

    private static void findClassesInDirectory(@NotNull File directory, @NotNull String packageName, @NotNull Map<@NotNull String, @NotNull InputStream> classes) throws IOException {
        // Retrieve directory files
        @NotNull File[] files = directory.listFiles();
        if (files == null) files = new File[0];

        // Read all files
        for (@NotNull File file : files) {
            if (file.isDirectory()) {
                findClassesInDirectory(file, packageName + file.getName() + ".", classes);
            } else if (file.getName().endsWith(".class")) {
                byte[] bytecode = convertInputStreamToByteArray(Files.newInputStream(file.toPath()));

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
                    byte[] bytecode = convertInputStreamToByteArray(jar.getInputStream(entry));

                    if (hasPluginType(bytecode)) {
                        @NotNull String name = entry.getName().replace("/", ".").replace(".class", "");
                        classes.put(name, new ByteArrayInputStream(bytecode));
                    }
                }
            }
        }
    }
    private static byte[] convertInputStreamToByteArray(@NotNull InputStream inputStream) throws IOException {
        @NotNull ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[2048];
        int bytesRead;

        while ((bytesRead = inputStream.read(buffer)) != -1) {
            byteArrayOutputStream.write(buffer, 0, bytesRead);
        }

        return byteArrayOutputStream.toByteArray();
    }

    // Object

    private Classes() {
        throw new UnsupportedOperationException("this class cannot be instantiated");
    }

}
