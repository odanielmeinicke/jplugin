package dev.meinicke.plugin.main;

import dev.meinicke.plugin.Builder;
import dev.meinicke.plugin.PluginInfo;
import dev.meinicke.plugin.annotation.Category;
import dev.meinicke.plugin.annotation.Dependency;
import dev.meinicke.plugin.annotation.Plugin;
import dev.meinicke.plugin.annotation.RequireMetadata;
import dev.meinicke.plugin.category.PluginCategory;
import dev.meinicke.plugin.context.PluginContext;
import dev.meinicke.plugin.exception.InvalidPluginException;
import dev.meinicke.plugin.exception.PluginInitializeException;
import dev.meinicke.plugin.exception.PluginInterruptException;
import dev.meinicke.plugin.factory.PluginFactory;
import dev.meinicke.plugin.factory.handlers.PluginHandler;
import dev.meinicke.plugin.metadata.type.MetadataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

final class PluginLoader {

    // Object

    private final @NotNull PluginFinderImpl finder;

    private @NotNull PluginInfo @Nullable [] plugins;
    private @Nullable ShutdownHookThread thread;

    public PluginLoader(@NotNull PluginFinderImpl finder) {
        this.finder = finder;
    }

    // Getters

    public @NotNull Class<?> getCallerClass() {
        // Retrieve caller class
        @NotNull StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        @Nullable Class<?> caller = null;

        if (stackTrace.length > 1) {
            int index = 0;
            for (@NotNull StackTraceElement element : stackTrace) {
                index++;
                if (index == 1) continue; // Skip first

                // Skip jplugin classes
                if (element.getClassName().startsWith("dev.meinicke.plugin")) {
                    continue;
                }

                // Select first
                try {
                    caller = Class.forName(element.getClassName());
                    break;
                } catch (ClassNotFoundException ignore1) {
                    if (!finder.getClassLoaders().isEmpty()) {
                        for (@NotNull ClassLoader loader : finder.getClassLoaders()) try {
                            caller = Class.forName(element.getClassName(), false, loader);
                            break;
                        } catch (ClassNotFoundException ignore2) {
                        }
                    }
                }

                if (caller == null) {
                    throw new RuntimeException("cannot retrieve caller class: " + element.getClassName());
                }
            }
        } else {
            throw new RuntimeException("invalid stack traces to retrieve caller class");
        }

        // Finish
        if (caller == null) {
            throw new IllegalStateException("cannot retrieve caller class");
        } else {
            return caller;
        }
    }

    public @NotNull PluginFinderImpl getFinder() {
        return finder;
    }
    public @NotNull PluginFactoryImpl getFactory() {
        return getFinder().getFactory();
    }

    public @Nullable Thread getThread() {
        return thread;
    }

    @Unmodifiable
    public @NotNull Collection<Class<?>> getClasses() throws IOException {
        // Classes
        @NotNull Set<Class<?>> references = new HashSet<>();

        // Consumer
        @NotNull Consumer<ClassData> consumer = data -> {
            // Variables
            @NotNull String name = data.getName();
            @NotNull InputStream inputStream = data.getInputStream();

            // Verify package
            @NotNull String pkg = name.contains(".") ? name.substring(0, name.lastIndexOf('.')) : "";
            if (!getFinder().checkPackageWithin(pkg)) return;

            // Load if it's a plugin
            for (@NotNull ClassLoader classLoader : finder.getClassLoaders()) {
                @Nullable Class<?> reference = null;
                try {
                    reference = data.loadIfPlugin(classLoader, finder);
                    if (reference != null) references.add(reference);
                } catch (ClassNotFoundException ignore) {
                }
            }
        };

        // Collect all references
//        if (finder.getClassLoaders().isEmpty()) {
//            @NotNull Class<?> caller = getCallerClass();
//            @NotNull URL url = caller.getProtectionDomain().getCodeSource().getLocation();
//
//            Classes.getAllTypeClassesWithVisitor(url, caller.getClassLoader(), consumer);
//        } else {
//            Classes.getAllTypeClassesWithVisitor(finder.getClassLoaders(), consumer);
//        }
        Classes.consumeAllClasses(consumer);

        // Finish
        return references;
    }

    public @NotNull Set<Builder> builders(@NotNull Predicate<Class<?>> predicate, @NotNull Map<Builder, Collection<PluginCategory>> handledCategories) throws IOException {
        // Variables
        @NotNull Class<?> caller = getCallerClass();
        @NotNull PluginFactory factory = Plugins.getPluginFactory();

        @NotNull Set<Builder> builders = new HashSet<>();

        // Select builders
        for (@NotNull Class<?> reference : getClasses()) {
            // Verifications
            if (!predicate.test(reference)) {
                continue;
            }

            // Check if it's an inner and non-class
            if (reference.getEnclosingClass() != null && !Modifier.isStatic(reference.getModifiers())) {
                throw new InvalidPluginException(reference, "a non-static inner class cannot be a plugin, the class should be at least static");
            }

            // Builder secondary variables
            @Nullable String name = reference.getAnnotation(Plugin.class).name();
            if (name.isEmpty()) name = null;

            @Nullable String description = reference.getAnnotation(Plugin.class).description();
            if (description.isEmpty()) description = null;

            // Create plugin context
            @NotNull PluginContext context = new PluginContextImpl(reference, caller, getFinder());
            getFinder().getMetadata().merge(context.getMetadata());

            // Initialize instance
            @NotNull Builder builder = new PluginBuilderImpl(Plugins.getInitializerFactory(), getFactory(), reference, context, name, description);

            // Call handlers
            boolean suppressed = false;

            handlers:
            for (@NotNull String categoryName : builder.getCategories()) {
                @Nullable PluginCategory category = factory.getCategory(categoryName, false).orElse(null);

                if (category != null) {
                    // Add it to handled categories to do not call the same handler twice later
                    handledCategories.putIfAbsent(builder, new HashSet<>());
                    if (handledCategories.get(builder).contains(category)) {
                        continue;
                    }

                    handledCategories.get(builder).add(category);

                    // Category handlers
                    if (!category.accept(builder)) {
                        suppressed = true;
                        break;
                    }

                    for (@NotNull PluginHandler handler : category.getHandlers()) {
                        try {
                            if (!handler.accept(builder)) {
                                suppressed = true;
                                break handlers;
                            }
                        } catch (Throwable throwable) {
                            throw new RuntimeException("cannot invoke category's handler to accept '" + category + "': " + handler);
                        }
                    }
                } else {
                    builder.category(categoryName);
                }
            }

            // Global Handlers
            if (suppressed) {
                continue;
            }

            for (@NotNull PluginHandler handler : Plugins.getPluginFactory().getGlobalHandlers()) {
                try {
                    if (!handler.accept(builder)) {
                        suppressed = true;
                        break;
                    }
                } catch (Throwable throwable) {
                    throw new RuntimeException("cannot invoke global handler to accept builder '" + builder.getReference() + "': " + handler);
                }
            }

            // Finish
            if (!suppressed) {
                builders.add(builder);
            }
        }

        // Finish
        return builders;
    }
    public @NotNull Collection<PluginInfo> load(@NotNull Predicate<Class<?>> predicate) throws PluginInitializeException, IOException {
        // Retrieve builders
        @NotNull Map<Builder, Collection<PluginCategory>> handledCategories = new HashMap<>();
        @NotNull Map<Builder, Collection<PluginCategory>> categories = new HashMap<>();

        @NotNull Set<Builder> builders = builders(predicate, handledCategories);
        @NotNull Set<Builder> refreshed = new HashSet<>();
        @NotNull Set<Builder> done = new HashSet<>();

        @NotNull Set<PluginInfo> plugins = new LinkedHashSet<>();

        // Shutdown hook
        @NotNull Set<PluginInfo> loadedPlugins = new LinkedHashSet<>();

        if (!builders.isEmpty() && getFinder().hasShutdownHook()) {
            @NotNull ShutdownHook hook = new ShutdownHook(loadedPlugins);
            Runtime.getRuntime().addShutdownHook(hook);
        }

        // Organize by dependencies order
        @NotNull Iterator<Builder> iterator = organizeBuilders(builders).iterator();

        main:
        while (iterator.hasNext()) {
            // Variables
            @NotNull Builder builder = iterator.next();
            @NotNull Class<?> reference = builder.getReference();
            categories.putIfAbsent(builder, new HashSet<>());
            handledCategories.putIfAbsent(builder, new HashSet<>());

            // Dependencies
            for (@NotNull Class<?> dependency : builder.getDependencies()) {
                if (builders.stream().noneMatch(b -> b.getReference().equals(dependency)) && getFactory().stream().noneMatch(b -> b.getReference().equals(dependency))) {
                    if (dependency.isAnnotationPresent(Plugin.class)) {
                        throw new InvalidPluginException(reference, "the plugin '" + reference.getName() + "' depends on '" + dependency.getName() + "' that isn't loaded.");
                    } else {
                        throw new InvalidPluginException(reference, "the plugin '" + reference.getName() + "' cannot have a dependency on '" + dependency.getName() + "' because it's not a plugin");
                    }
                }
            }

            // Categories
            for (@NotNull Category annotation : reference.getAnnotationsByType(Category.class)) {
                @Nullable PluginCategory category = getFactory().getCategory(annotation.value(), false).orElse(null);

                if (category != null) {
                    if (handledCategories.get(builder).contains(category)) {
                        continue;
                    }

                    builder.category(category);
                    categories.computeIfAbsent(builder, k -> new LinkedList<>()).add(category);
                    handledCategories.get(builder).add(category);

                    // Category handlers
                    if (callCategory(builder, category)) {
                        continue main;
                    }
                } else {
                    builder.category(annotation.value());
                }
            }

            if (!refreshed.contains(builder)) {
                refreshed.add(builder);

                @NotNull Set<Builder> next = new LinkedHashSet<>(builders);
                next.removeAll(done);

                iterator = organizeBuilders(next).iterator();

                continue;
            }

            // Call global handlers
            for (@NotNull PluginHandler handler : getFactory().getGlobalHandlers()) {
                if (!handler.accept(builder)) {
                    continue main;
                }
            }

            // Build
            @NotNull PluginInfo plugin;

            try {
                plugin = builder.build();
            } catch (Throwable e) {
                throw new PluginInitializeException(reference, "cannot build plugin info of class: " + reference.getName(), e);
            }

            // Call Handlers
            {
                // Category handlers
                for (@NotNull PluginCategory category : categories.get(builder)) {
                    // Category handlers
                    if (callCategory(plugin, category)) {
                        continue main;
                    }
                }

                // Global handlers
                for (@NotNull PluginHandler handler : getFactory().getGlobalHandlers()) {
                    if (!handler.accept(plugin)) {
                        continue main;
                    }
                }
            }

            // Register it
            getFactory().plugins.put(reference, plugin);
            plugins.add(plugin);

            // Change context variables
            @NotNull PluginContextImpl context = (PluginContextImpl) builder.getContext();
            context.plugin = plugin;
            context.plugins.addAll(plugins);

            // Check metadata
            for (@NotNull RequireMetadata annotation : plugin.getReference().getAnnotationsByType(RequireMetadata.class)) {
                @NotNull String key = annotation.key();
                @Nullable Object value = getFinder().getMetadata().getOrDefault(key, null);

                @NotNull Class<?> type = annotation.type();

                @Nullable MetadataType<?> metadataType = getFinder().getMetadata().getType(key).orElse(null);

                if (!getFinder().getMetadata().containsKey(key)) {
                    throw new IllegalStateException("to load the plugin \"" + plugin.getReference().getName() + "\" it should have a metadata with key \"" + key + "\" and type:" + type.getName());
                } else if (metadataType != null && !metadataType.getReference().isAssignableFrom(type)) {
                    throw new IllegalStateException("the annotation type \"" + type + "\" must have an assignable type with \"" + metadataType.getReference().getName() + "\" for key \"" + key + "\" from plugin: " + plugin.getReference().getName());
                } else if (value != null && !type.isAssignableFrom(value.getClass())) {
                    throw new IllegalStateException("the metadata key \"" + key + "\" must have an assignable type with \"" + type.getName() + "\" from plugin: " + plugin.getReference().getName());
                }
            }

            // Start plugin
            try {
                plugin.start();

                if (plugin.isAutoClose()) {
                    loadedPlugins.add(plugin);
                }
            } catch (PluginInitializeException e) {
                throw e;
            } catch (Throwable throwable) {
                throw new PluginInitializeException(plugin.getReference(), "cannot initialize plugin correctly", throwable);
            }

            // Refresh iterator
            done.add(builder);

            @NotNull Set<Builder> next = new LinkedHashSet<>(builders);
            next.removeAll(done);

            iterator = organizeBuilders(next).iterator();
        }

        // Add dependencies
        for (@NotNull PluginInfo plugin : plugins) {
            @NotNull List<@NotNull PluginInfo> dependants = plugins.stream().filter(target -> target.getDependencies().contains(plugin)).collect(Collectors.toList());
            plugin.getDependants().addAll(dependants);
        }

        // Finish
        return plugins;
    }

    public static @NotNull Set<PluginInfo> organizePlugins(@NotNull Set<PluginInfo> plugins) {
        @NotNull Map<Class<?>, PluginInfo> map = plugins.stream().collect(Collectors.toMap(PluginInfo::getReference, Function.identity()));
        @NotNull Set<Class<?>> sortedReferences = organize(map.keySet());

        @NotNull Set<PluginInfo> sorted = new LinkedHashSet<>();
        for (@NotNull Class<?> reference : sortedReferences) {
            PluginInfo info = map.get(reference);
            if (info == null) {
                throw new IllegalStateException("Missing PluginInfo for reference: " + reference);
            }
            sorted.add(info);
        }

        return sorted;
    }
    private static @NotNull Set<Builder> organizeBuilders(@NotNull Set<Builder> builders) {
        // Organize builders by order
        @NotNull Set<Builder> sorted = new LinkedHashSet<>();
        @NotNull List<Builder> remaining = new LinkedList<>(builders);
        @NotNull Map<Class<?>, Builder> builderByReference = builders.stream().collect(Collectors.toMap(Builder::getReference, Function.identity()));

        while (!remaining.isEmpty()) {
            @NotNull List<Builder> eligible = new LinkedList<>();

            for (@NotNull Builder builder : remaining) {
                boolean ready = true;

                for (@NotNull Class<?> dependency : builder.getDependencies()) {
                    @NotNull Builder dependencyBuilder = builderByReference.get(dependency);
                    if (dependencyBuilder != null && !sorted.contains(dependencyBuilder)) {
                        ready = false;
                        break;
                    }
                }

                if (ready) {
                    eligible.add(builder);
                }
            }

            if (eligible.isEmpty()) {
                throw new IllegalStateException("cyclic or unresolved dependencies detected: " + remaining);
            }

            eligible.sort(Comparator.comparingInt(Builder::getPriority));

            @NotNull Builder chosen = eligible.get(0);
            sorted.add(chosen);
            remaining.remove(chosen);
        }

        // Finish
        return sorted;
    }
    private static @NotNull Set<Class<?>> organize(@NotNull Set<Class<?>> references) {
        @NotNull Set<Class<?>> sorted = new LinkedHashSet<>();
        @NotNull List<Class<?>> remaining = new ArrayList<>(references);

        boolean progress;
        do {
            progress = false;
            @NotNull Iterator<Class<?>> iterator = remaining.iterator();

            while (iterator.hasNext()) {
                @NotNull Class<?> reference = iterator.next();
                @NotNull Collection<Class<?>> dependencies = Arrays.stream(reference.getAnnotationsByType(Dependency.class)).map(Dependency::type).collect(Collectors.toList());

                if (dependencies.isEmpty() || sorted.containsAll(dependencies)) {
                    sorted.add(reference);
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

    // Classes

    private static final class ShutdownHookThread extends Thread {

        // Static initializers

        private static final @NotNull AtomicInteger THREAD_COUNT = new AtomicInteger(0);

        // Object

        private final @NotNull Collection<PluginInfo> plugins;

        public ShutdownHookThread(@NotNull Collection<PluginInfo> plugins) {
            super("Plug-ins Shutdown Hook #" + THREAD_COUNT.getAndIncrement());
            this.plugins = plugins;
        }

        // Getters

        private @NotNull Collection<PluginInfo> getPlugins() {
            return plugins;
        }

        // Modules

        @Override
        public void run() {
            @NotNull List<PluginInfo> plugins = new LinkedList<>(getPlugins());
            Collections.reverse(plugins);

            // Loop
            for (@NotNull PluginInfo info : plugins) {
                if (!info.isAutoClose()) {
                    continue;
                } else if (info.getState() != PluginInfo.State.RUNNING) {
                    continue;
                }

                try {
                    info.close();
                } catch (PluginInterruptException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    // Utilities

    private static boolean callCategory(@NotNull Builder builder, @NotNull PluginCategory category) {
        if (!category.accept(builder)) {
            return true;
        }

        // Handlers
        for (@NotNull PluginHandler handler : category.getHandlers()) {
            try {
                if (!handler.accept(builder)) {
                    return true;
                }
            } catch (Throwable throwable) {
                throw new RuntimeException("cannot invoke category's handler to accept '" + category + "': " + handler);
            }
        }

        // Finish
        return false;
    }
    private static boolean callCategory(@NotNull PluginInfo plugin, @NotNull PluginCategory category) {
        if (!category.accept(plugin)) {
            return true;
        }

        // Handlers
        for (@NotNull PluginHandler handler : category.getHandlers()) {
            try {
                if (!handler.accept(plugin)) {
                    return true;
                }
            } catch (Throwable throwable) {
                throw new RuntimeException("cannot invoke category's handler to accept '" + category + "': " + handler);
            }
        }

        // Finish
        return false;
    }

    // Classes

    private static final class ShutdownHook extends Thread {

        // Object

        private final @NotNull Collection<PluginInfo> plugins;

        public ShutdownHook(@NotNull Collection<PluginInfo> plugins) {
            super("Plug-ins Shutdown Hook");
            this.plugins = plugins;
        }

        // Getters

        private @NotNull Collection<PluginInfo> getPlugins() {
            return plugins;
        }

        // Modules

        @Override
        public void run() {
            @NotNull List<PluginInfo> plugins = new LinkedList<>(getPlugins());
            Collections.reverse(plugins);

            // Loop
            for (@NotNull PluginInfo info : plugins) {
                if (!info.isAutoClose()) {
                    continue;
                } else if (info.getState() != PluginInfo.State.RUNNING) {
                    continue;
                }

                try {
                    info.close();
                } catch (PluginInterruptException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

}
