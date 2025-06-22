package dev.meinicke.plugin.main;

import dev.meinicke.plugin.annotation.Category;
import dev.meinicke.plugin.annotation.Dependency;
import dev.meinicke.plugin.annotation.Initializer;
import dev.meinicke.plugin.annotation.Plugin;
import dev.meinicke.plugin.initializer.PluginInitializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

final class ClassData implements Closeable {

    private final @NotNull String name;
    private final @NotNull InputStream inputStream;

    public ClassData(@NotNull String name, @NotNull InputStream inputStream) {
        this.name = name;
        this.inputStream = inputStream;
    }

    // Getters

    public @NotNull String getName() {
        return name;
    }
    public @NotNull InputStream getInputStream() {
        return inputStream;
    }

    // Modules

    public @Nullable Class<?> loadIfPlugin(@NotNull ClassLoader classLoader, @NotNull PluginFinderImpl finder) throws ClassNotFoundException {
        try {
            @NotNull Class<?> reference = Class.forName(name, false, classLoader);

            if (reference.isAnnotationPresent(Plugin.class) && (finder.getClassLoaders().isEmpty() || finder.getClassLoaders().contains(reference.getClassLoader()))) {
                return reference;
            }
        } catch (ClassNotFoundException | @NotNull NoClassDefFoundError e) {
            double classVersion = Double.parseDouble(System.getProperty("java.class.version"));

            int opcodeTemp;
            if (classVersion >= 59) opcodeTemp = Opcodes.ASM9;
            else if (classVersion >= 57) opcodeTemp = Opcodes.ASM8;
            else if (classVersion >= 55) opcodeTemp = Opcodes.ASM7;
            else if (classVersion >= 53) opcodeTemp = Opcodes.ASM6;
            else if (classVersion == 52) opcodeTemp = Opcodes.ASM5;
            else opcodeTemp = Opcodes.ASM4;
            final int opcode = opcodeTemp;

            try {
                @NotNull AtomicBoolean plugin = new AtomicBoolean(false);
                @NotNull AtomicBoolean valid = new AtomicBoolean(true);

                @NotNull ClassReader reader = new ClassReader(inputStream);
                @NotNull ClassVisitor visitor = new ClassVisitor(opcode) {
                    @Override
                    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                        final boolean isPlugin = descriptor.contains(Plugin.class.getName().replace('.', '/'));
                        if (isPlugin) plugin.set(true);

                        if (valid.get()) {
                            return new AnnotationVisitor(opcode) {
                                @Override
                                public void visit(String name, Object value) {
                                    if (isPlugin) {
                                        if (!finder.getNames().isEmpty() && name.equals("name") && !finder.getNames().contains(value.toString())) {
                                            valid.set(false);
                                        } else if (!finder.getDescriptions().isEmpty() && name.equals("description") && !finder.getDescriptions().contains(value.toString())) {
                                            valid.set(false);
                                        }
                                    } else if (descriptor.contains(Category.class.getName().replace('.', '/'))) {
                                        if (!finder.getCategories().isEmpty() && name.equals("name") && finder.getCategories().stream().noneMatch(category -> category.equalsIgnoreCase(value.toString()))) {
                                            valid.set(false);
                                        }
                                    } else if (descriptor.contains(Initializer.class.getName().replace('.', '/'))) {
                                        //noinspection unchecked
                                        if (!finder.getInitializers().isEmpty() && name.equals("type") && !finder.getInitializers().contains((Class<? extends PluginInitializer>) value)) {
                                            valid.set(false);
                                        }
                                    } else if (descriptor.contains(Dependency.class.getName().replace('.', '/'))) {
                                        if (!finder.getDependencies().isEmpty() && name.equals("type") && !finder.getDependencies().contains((Class<?>) value)) {
                                            valid.set(false);
                                        }
                                    }

                                    super.visit(name, value);
                                }
                            };
                        }

                        return super.visitAnnotation(descriptor, visible);
                    }
                };

                reader.accept(visitor, ClassReader.EXPAND_FRAMES);

                if (plugin.get() && valid.get()) {
                    return classLoader.loadClass(name);
                }
            } catch (IOException ignore) {
            }
        }

        // Not a valid plugin
        return null;
    }

    // Implementations


    @Override
    public @NotNull String toString() {
        return getName();
    }

    // Closeable

    @Override
    public void close() throws IOException {
        getInputStream().close();
    }

}
