package codes.laivy.plugin.main;

import codes.laivy.plugin.PluginInfo;
import codes.laivy.plugin.PluginMain;
import codes.laivy.plugin.annotation.Dependency;
import codes.laivy.plugin.annotation.Plugin;
import codes.laivy.plugin.exception.PluginInitializeException;
import codes.laivy.plugin.exception.PluginInterruptException;
import codes.laivy.plugin.utilities.Classes;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public final class Plugins {

    // Static initializers

    static {
        Runtime.getRuntime().addShutdownHook(new ShutdownHook());
    }

    static final @NotNull Set<PluginInfo> plugins = new LinkedHashSet<>();

    public static @NotNull PluginInfo retrieve(@NotNull Class<?> reference) {
        return plugins.stream().filter(plugin -> plugin.getReference().equals(reference)).findFirst().orElseThrow(() -> new IllegalArgumentException("the class '" + reference.getName() + "' isn't a plugin"));
    }
    public static @NotNull PluginInfo retrieve(@NotNull String name) {
        return plugins.stream().filter(plugin -> Objects.equals(plugin.getName(), name)).findFirst().orElseThrow(() -> new IllegalArgumentException("there's no plugin with name '" + name + "'"));
    }
    
    public static void initialize(@NotNull ClassLoader loader, @NotNull String packge, boolean recurring) throws PluginInitializeException, IOException {
        // Variables
        @NotNull Map<@NotNull Class<?>, @NotNull Collection<Class<?>>> references = new HashMap<>();

        // Retrieve all plugins
        @NotNull Enumeration<URL> enumeration = PluginMain.class.getClassLoader().getResources(packge.replace(".", File.separator));

        while (enumeration.hasMoreElements()) {
            @NotNull URL url = enumeration.nextElement();

            for (@NotNull String path : new String(toByteArray(url.openStream())).split("\n")) {
                if (!path.endsWith(".class")) {
                    continue;
                }

                @NotNull String name = packge + "." + path.replace(".class", "");
                @NotNull Class<?> reference;

                try {
                    reference = Class.forName(name);
                } catch (@NotNull ClassNotFoundException e) {
                    continue;
                }

                if (reference.isAnnotationPresent(Plugin.class)) {
                    // Resolve dependencies
                    @NotNull List<Class<?>> dependencies = new LinkedList<>();

                    for (@NotNull Dependency dependency : reference.getAnnotationsByType(Dependency.class)) {
                        dependencies.add(dependency.type());
                    }

                    references.put(reference, dependencies);
                }
            }
        }

        // Load
        load(references);
    }
    public static void initialize(@NotNull String packge, boolean recurring) throws PluginInitializeException, IOException {
        initialize(Thread.currentThread().getContextClassLoader(), packge, recurring);
    }
    public static void initialize(@NotNull ClassLoader loader, @NotNull Package packge, boolean recurring) throws PluginInitializeException, IOException {
        initialize(loader, packge.getName(), recurring);
    }
    public static void initialize(@NotNull Package packge, boolean recurring) throws PluginInitializeException, IOException {
        initialize(Thread.currentThread().getContextClassLoader(), packge.getName(), recurring);
    }

    public static void interrupt(@NotNull ClassLoader loader) throws PluginInterruptException {
        @NotNull List<PluginInfo> plugins = new LinkedList<>(organize(Plugins.plugins));
        Collections.reverse(plugins);

        for (@NotNull PluginInfo info : plugins) {
            // todo: check dependants
            if (info.getReference().getClassLoader().equals(loader)) {
                info.close();
            }
        }
    }
    @ApiStatus.Experimental
    public static void initialize(@NotNull ClassLoader loader) throws PluginInitializeException, IOException {
        // Variables
        @NotNull Map<@NotNull Class<?>, @NotNull Collection<Class<?>>> references = new HashMap<>();

        // Retrieve classes
        @NotNull Enumeration<@NotNull URL> enumeration = loader.getResources("");
        @NotNull Map<@NotNull String, @NotNull URL> files = new LinkedHashMap<>();

        while (enumeration.hasMoreElements()) {
            @NotNull URL url = enumeration.nextElement();
            files.putAll(Classes.read(loader, url));
        }

        // Load plugins
        for (@NotNull Map.Entry<@NotNull String, @NotNull URL> entry : files.entrySet()) {
            // Variables
            @NotNull String name = entry.getKey()
                    .replace(".class", "")
                    .replace(File.separator, ".");
            @NotNull URL url = entry.getValue();

            // Retrieve references
            @NotNull Class<?> reference;

            try {
                reference = Class.forName(name);
            } catch (@NotNull ClassNotFoundException e) {
                continue;
            }

            if (reference.isAnnotationPresent(Plugin.class)) {
                // Resolve dependencies
                @NotNull List<Class<?>> dependencies = new LinkedList<>();

                for (@NotNull Dependency dependency : reference.getAnnotationsByType(Dependency.class)) {
                    dependencies.add(dependency.type());
                }

                references.put(reference, dependencies);
            }
        }

        // Finish
        load(references);
    }

    @ApiStatus.Experimental
    public static void initializeAll() throws PluginInitializeException, IOException {
        // Variables
        @NotNull Map<@NotNull Class<?>, @NotNull Collection<Class<?>>> references = new HashMap<>();

        // Retrieve all plugins
        for (@NotNull Map.Entry<@NotNull String, @NotNull InputStream> entry : Classes.getAllPluginTypeClasses().entrySet()) {
            @NotNull String name = entry.getKey();
            @NotNull InputStream stream = entry.getValue();

            // Check bytes
            @NotNull Class<?> reference;

            try {
                // Check if class exists
                reference = Class.forName(name);
            } catch (@NotNull ClassNotFoundException ignore) {
                // Define class
                reference = define(toByteArray(stream));
            }

            // Resolve dependencies
            @NotNull List<Class<?>> dependencies = new LinkedList<>();

            for (@NotNull Dependency dependency : reference.getAnnotationsByType(Dependency.class)) {
                dependencies.add(dependency.type());
            }

            // Initialize plugin
            references.put(reference, dependencies);
        }

        // Load references
        load(references);
    }
    public static void interruptAll() throws PluginInterruptException {
        @NotNull List<PluginInfo> plugins = new LinkedList<>(organize(Plugins.plugins));
        Collections.reverse(plugins);

        for (@NotNull PluginInfo info : plugins) {
            info.close();
        }
    }

    private static void load(@NotNull Map<@NotNull Class<?>, @NotNull Collection<Class<?>>> references) throws PluginInitializeException {
        // Create instances
        @NotNull Map<@NotNull Class<?>, @NotNull PluginInfo> plugins = new LinkedHashMap<>();

        for (@NotNull Map.Entry<@NotNull Class<?>, @NotNull Collection<Class<?>>> entry : references.entrySet()) {
            @NotNull Class<?> reference = entry.getKey();
            @NotNull List<Class<?>> dependencies = new LinkedList<>();

            // Dependencies
            for (@NotNull Class<?> plugin : entry.getValue()) {
                // Check issues
                if (!references.containsKey(plugin)) {
                    throw new PluginInitializeException(reference, "the dependency '" + plugin.getName() + "' is not a plugin");
                } else if (plugin == reference) {
                    throw new PluginInitializeException(reference, "the reference cannot have a dependency on itself");
                }

                // Generate instance
                dependencies.add(plugin);
            }

            // Name
            @Nullable String name = reference.getAnnotation(Plugin.class).name();
            if (name != null && name.isEmpty()) name = null;

            // Create instance
            @NotNull PluginInfo plugin = new PluginInfoImpl(name, reference, dependencies.toArray(new Class[0]));
            plugins.put(reference, plugin);
        }

        // Organize by dependencies order
        for (@NotNull PluginInfo plugin : organize(plugins.values())) {
            try {
                plugin.start();
            } catch (@NotNull PluginInitializeException e) {
                throw e;
            } catch (@NotNull Throwable throwable) {
                throw new PluginInitializeException(plugin.getReference(), "cannot initialize plugin correctly", throwable);
            }
        }
    }
    private static @NotNull Set<PluginInfo> organize(@NotNull Collection<@NotNull PluginInfo> plugins) {
        @NotNull Set<PluginInfo> sorted = new LinkedHashSet<>();
        @NotNull List<PluginInfo> remaining = new ArrayList<>(plugins);

        boolean progress;
        do {
            progress = false;
            @NotNull Iterator<PluginInfo> iterator = remaining.iterator();

            while (iterator.hasNext()) {
                @NotNull PluginInfo plugin = iterator.next();
                @NotNull List<PluginInfo> dependencies = Arrays.asList(plugin.getDependencies());

                if (dependencies.isEmpty() || sorted.containsAll(dependencies)) {
                    sorted.add(plugin);
                    iterator.remove();
                    progress = true;
                }
            }
        } while (progress);

        if (!remaining.isEmpty()) {
            throw new IllegalStateException("cyclic or unresolved dependencies detected: " + remaining);
        }

        return sorted;
    }

    // Object

    private Plugins() {
        throw new UnsupportedOperationException("this class cannot be instantiated");
    }
    
    // Classes

    private static final class ShutdownHook extends Thread {

        // Object

        public ShutdownHook() {
            super("Plug-ins Shutdown Hook");
        }

        // Modules

        @Override
        public void run() {
            try {
                interruptAll();
            } catch (@NotNull PluginInterruptException e) {
                throw new RuntimeException(e);
            }
        }

    }

    // Utilities

    private static @NotNull Class<?> define(byte[] bytes) {
        try {
            @NotNull Method define = ClassLoader.class.getDeclaredMethod("defineClass", byte[].class, int.class, int.class);
            define.setAccessible(true);
            return (Class<?>) define.invoke(PluginMain.class.getClassLoader(), bytes, 0, bytes.length);
        } catch (@NotNull NoSuchMethodException e) {
            throw new RuntimeException("cannot find ClassLoader's #defineClass(byte[], int, int) method", e);
        } catch (@NotNull IllegalAccessException e) {
            throw new RuntimeException("cannot access ClassLoader's #defineClass(byte[], int, int) method", e);
        } catch (@NotNull InvocationTargetException e) {
            throw new RuntimeException("cannot invoke ClassLoader's #defineClass(byte[], int, int) method", e);
        }
    }
    private static boolean hasPluginType(byte[] bytecode) {
        @NotNull String interfaceName = codes.laivy.plugin.annotation.Plugin.class.getName().replace('.', '/');
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
    // todo: made this not public
    public static byte[] toByteArray(@NotNull InputStream input) throws IOException {
        @NotNull ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[2048];

        int bytesRead;
        while ((bytesRead = input.read(data)) != -1) {
            buffer.write(data, 0, bytesRead);
        }

        return buffer.toByteArray();
    }

}
