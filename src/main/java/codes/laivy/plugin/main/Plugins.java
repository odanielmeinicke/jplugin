package codes.laivy.plugin.main;

import codes.laivy.plugin.PluginInfo;
import codes.laivy.plugin.annotation.Dependency;
import codes.laivy.plugin.annotation.Plugin;
import codes.laivy.plugin.exception.InvalidPluginException;
import codes.laivy.plugin.exception.PluginInitializeException;
import codes.laivy.plugin.exception.PluginInterruptException;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

public final class Plugins {

    // Static initializers

    static {
        Runtime.getRuntime().addShutdownHook(new ShutdownHook());
    }

    static final @NotNull Map<Class<?>, PluginInfoImpl> plugins = new LinkedHashMap<>();

    public static @NotNull PluginInfo retrieve(@NotNull Class<?> reference) {
        return plugins.values().stream().filter(plugin -> plugin.getReference().equals(reference)).findFirst().orElseThrow(() -> new IllegalArgumentException("the class '" + reference.getName() + "' isn't a plugin"));
    }
    public static @NotNull PluginInfo retrieve(@NotNull String name) {
        return plugins.values().stream().filter(plugin -> Objects.equals(plugin.getName(), name)).findFirst().orElseThrow(() -> new IllegalArgumentException("there's no plugin with name '" + name + "'"));
    }

    public static void interrupt(@NotNull ClassLoader loader, @NotNull String packge, boolean recursive) throws PluginInterruptException {
        @NotNull List<PluginInfo> plugins = new LinkedList<>(organize(Plugins.plugins.values()));
        Collections.reverse(plugins);

        for (@NotNull PluginInfo info : plugins) {
            if (info.getReference().getClassLoader().equals(loader) && !Classes.isPackageWithin(packge, info.getReference().getPackage().getName(), recursive)) {
                continue;
            }

            info.close();
        }
    }
    public static void initialize(@NotNull ClassLoader loader, @NotNull String packge, boolean recursive) throws PluginInitializeException, IOException {
        // Variables
        @NotNull Set<@NotNull Class<?>> references = new LinkedHashSet<>();

        // Retrieve all plugins
        @NotNull Enumeration<URL> enumeration = loader.getResources(packge.replace(".", File.separator));

        while (enumeration.hasMoreElements()) {
            @NotNull URL url = enumeration.nextElement();

            for (@NotNull String path : new String(Classes.toByteArray(url.openStream())).split("\n")) {
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
                    // Add plugin to references, and load it later.
                    references.add(reference);
                }
            }
        }

        // Load
        load(references);
    }

    public static void interrupt(@NotNull String packge, boolean recursive) throws PluginInterruptException {
        interrupt(Thread.currentThread().getContextClassLoader(), packge, recursive);
    }
    public static void initialize(@NotNull String packge, boolean recursive) throws PluginInitializeException, IOException {
        initialize(Thread.currentThread().getContextClassLoader(), packge, recursive);
    }

    public static void interrupt(@NotNull ClassLoader loader, @NotNull Package packge, boolean recursive) throws PluginInterruptException {
        interrupt(loader, packge.getName(), recursive);
    }
    public static void initialize(@NotNull ClassLoader loader, @NotNull Package packge, boolean recursive) throws PluginInitializeException, IOException {
        initialize(loader, packge.getName(), recursive);
    }

    public static void interrupt(@NotNull Package packge, boolean recursive) throws PluginInterruptException {
        interrupt(Thread.currentThread().getContextClassLoader(), packge.getName(), recursive);
    }
    public static void initialize(@NotNull Package packge, boolean recursive) throws PluginInitializeException, IOException {
        initialize(Thread.currentThread().getContextClassLoader(), packge.getName(), recursive);
    }

    public static void interrupt(@NotNull ClassLoader loader) throws PluginInterruptException {
        @NotNull List<PluginInfo> plugins = new LinkedList<>(organize(Plugins.plugins.values()));
        Collections.reverse(plugins);

        for (@NotNull PluginInfo info : plugins) {
            @NotNull Class<?> reference = info.getReference();

            if (reference.getClassLoader().equals(loader)) {
                info.close();
            }
        }
    }
    @ApiStatus.Experimental
    public static void initialize(@NotNull ClassLoader loader) throws PluginInitializeException, IOException {
        // Variables
        @NotNull Set<@NotNull Class<?>> references = new LinkedHashSet<>();

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

            // Add plugin to references, and load it later.
            if (reference.isAnnotationPresent(Plugin.class)) {
                references.add(reference);
            }
        }

        // Finish
        load(references);
    }

    @ApiStatus.Experimental
    public static void initializeAll() throws PluginInitializeException, IOException {
        // Variables
        @NotNull Set<@NotNull Class<?>> references = new LinkedHashSet<>();

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
                reference = Classes.define(Classes.toByteArray(stream));
            }

            // Add plugin to references, and load it later.
            references.add(reference);
        }

        // Load references
        load(references);
    }
    public static void interruptAll() throws PluginInterruptException {
        @NotNull List<PluginInfo> plugins = new LinkedList<>(organize(Plugins.plugins.values()));
        Collections.reverse(plugins);

        for (@NotNull PluginInfo info : plugins) {
            info.close();
        }
    }

    private static void load(@NotNull Collection<@NotNull Class<?>> references) throws PluginInitializeException {
        // Create instances
        @NotNull Map<@NotNull Class<?>, @NotNull PluginInfoImpl> plugins = new LinkedHashMap<>();

        for (@NotNull Class<?> reference : references) {
            // Check if it's an inner and non-static class
            if (reference.getEnclosingClass() != null && !Modifier.isStatic(reference.getModifiers())) {
                throw new InvalidPluginException(reference, "a non-static inner class cannot be a plugin, the class should be atleast static");
            }

            // Dependencies
            @NotNull Set<Class<?>> dependencies = new LinkedHashSet<>();

            for (@NotNull Dependency annotation : reference.getAnnotationsByType(Dependency.class)) {
                @NotNull Class<?> dependency = annotation.type();

                // Check issues
                if (!Plugins.plugins.containsKey(dependency)) {
                    throw new InvalidPluginException(reference, "there's a dependency that is not a plugin at '" + reference.getName() + "': " + dependency.getName());
                } else if (dependency == reference) {
                    throw new InvalidPluginException(reference, "the plugin cannot have a dependency on itself");
                }

                // Generate instance
                dependencies.add(dependency);
            }

            // Name
            @Nullable String name = reference.getAnnotation(Plugin.class).name();
            if (name != null && name.isEmpty()) name = null;

            // Create instance and register it
            @NotNull PluginInfoImpl plugin = new PluginInfoImpl(name, reference, dependencies.toArray(new Class[0]));
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

        // Add dependencies
        for (@NotNull PluginInfoImpl plugin : plugins.values()) {
            @NotNull List<@NotNull PluginInfoImpl> dependants = plugins.values().stream().filter(target -> target.getDependencies().contains(plugin)).collect(Collectors.toList());
            plugin.dependants.addAll(dependants);
        }
    }
    private static @NotNull Set<PluginInfo> organize(@NotNull Collection<@NotNull PluginInfoImpl> plugins) {
        @NotNull Set<PluginInfo> sorted = new LinkedHashSet<>();
        @NotNull List<PluginInfo> remaining = new ArrayList<>(plugins);

        boolean progress;
        do {
            progress = false;
            @NotNull Iterator<PluginInfo> iterator = remaining.iterator();

            while (iterator.hasNext()) {
                @NotNull PluginInfo plugin = iterator.next();
                @NotNull Collection<PluginInfo> dependencies = plugin.getDependencies();

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
    
}
