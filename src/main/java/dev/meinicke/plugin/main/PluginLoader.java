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
import dev.meinicke.plugin.factory.PluginFactory;
import dev.meinicke.plugin.factory.handlers.PluginHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static dev.meinicke.plugin.main.PluginLoader.HandlerState.SUPPRESSED;

final class PluginLoader {

    // Static initializers

    private static final @NotNull Logger log = LoggerFactory.getLogger(PluginLoader.class);

    // Object

    private final @NotNull PluginFinderImpl finder;

    private @Nullable ShutdownHookThread thread;

    // Loading variables

    private volatile boolean loaded;

    private final @NotNull Map<Class<?>, Set<PluginHandler>> builderHandlers = new HashMap<>();
    private final @NotNull Map<Class<?>, Set<PluginHandler>> pluginHandlers = new HashMap<>();

    private final @NotNull Set<Builder> builders;
    private final @NotNull Set<PluginInfo> plugins = new LinkedHashSet<>();

    public PluginLoader(@NotNull PluginFinderImpl finder, @NotNull Predicate<Class<?>> predicate) throws IOException {
        this.finder = finder;

        // Variables
        @NotNull Class<?> caller = getCallerClass();
        @NotNull PluginFactory factory = Plugins.getPluginFactory();

        @NotNull Set<Builder> builders = new LinkedHashSet<>();

        // Select builders
        for (@NotNull Class<?> reference : getClasses()) {
            // Verifications
            if (!predicate.test(reference)) {
                continue;
            }

            // Check if it's an inner and non-class
            if (reference.getEnclosingClass() != null && !Modifier.isStatic(reference.getModifiers())) {
                throw new InvalidPluginException(reference, "a non-static inner class cannot be a plugin, the class should be at least static: " + reference.getName());
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
            builders.add(builder);
        }

        // Finish
        this.builders = organizeBuilders(builders);

        // Verify dependencies
        for (@NotNull Builder builder : getBuilders()) {
            @NotNull Class<?> reference = builder.getReference();

            for (@NotNull Class<?> dependency : builder.getDependencies()) {
                if (getBuilders().stream().noneMatch(b -> b.getReference().equals(dependency)) && getFactory().stream().noneMatch(b -> b.getReference().equals(dependency))) {
                    if (dependency.isAnnotationPresent(Plugin.class)) {
                        throw new InvalidPluginException(reference, "the plugin '" + reference.getName() + "' depends on '" + dependency.getName() + "' that isn't loaded.");
                    } else {
                        throw new InvalidPluginException(reference, "the plugin '" + reference.getName() + "' cannot have a dependency on '" + dependency.getName() + "' because it's not a plugin");
                    }
                }
            }
        }
    }

    // Getters

    public @NotNull PluginFinderImpl getFinder() {
        return finder;
    }
    public @NotNull PluginFactoryImpl getFactory() {
        return getFinder().getFactory();
    }

    public @Nullable ShutdownHookThread getThread() {
        return thread;
    }

    private @NotNull Set<Builder> getBuilders() {
        return builders;
    }
    public @NotNull Set<PluginInfo> getPlugins() {
        return plugins;
    }

    public boolean alreadyAcceptedHandler(@NotNull Builder builder, @NotNull PluginHandler handler) {
        @NotNull Class<?> reference = builder.getReference();
        builderHandlers.putIfAbsent(reference, new HashSet<>());

        if (!builderHandlers.get(reference).contains(handler)) {
            builderHandlers.get(reference).add(handler);
            return false;
        }

        return true;
    }
    public boolean alreadyAcceptedHandler(@NotNull PluginInfo plugin, @NotNull PluginHandler handler) {
        @NotNull Class<?> reference = plugin.getReference();
        pluginHandlers.putIfAbsent(reference, new HashSet<>());

        if (!pluginHandlers.get(reference).contains(handler)) {
            pluginHandlers.get(reference).add(handler);
            return false;
        }

        return true;
    }

    // Modules

    private void callEveryoneAgain(@NotNull Set<Builder> builders) {
        builders.removeIf(builder -> callAcceptHandlers(builder) == SUPPRESSED);
    }

    public synchronized void load() throws PluginInitializeException {
        // Variables
        @NotNull Set<Builder> builders = new LinkedHashSet<>(getBuilders());
        @NotNull Set<PluginInfo> plugins = new LinkedHashSet<>();

        // The first handlers call is necessary to apply the default categories and handlers (Like "Category Reference")
        callEveryoneAgain(builders);

        // Start building plugins
        @NotNull Iterator<Builder> iterator = organizeBuilders(builders).iterator();

        while (iterator.hasNext()) {
            @NotNull Builder builder = iterator.next();
            @NotNull Class<?> reference = builder.getReference();

            @Nullable HandlerState state = callAcceptHandlers(builder);

            if (state == SUPPRESSED) {
                builders.remove(builder);
            } else if (state == null) {
                // Build plugin
                @Nullable PluginInfo plugin = plugins.stream().filter(p -> p.getReference().equals(reference)).findFirst().orElse(null);

                if (plugin == null) {
                    try {
                        plugin = builder.build();
                    } catch (@NotNull Throwable e) {
                        throw new PluginInitializeException(reference, "cannot build plugin info of class: " + reference.getName(), e);
                    }

                    // Register it
                    getFactory().plugins.put(reference, plugin);

                    // Add it to the list
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

                        if (!getFinder().getMetadata().containsKey(key)) {
                            throw new IllegalStateException("to load the plugin \"" + plugin.getReference().getName() + "\" it should have a metadata with key \"" + key + "\" and type:" + type.getName());
                        } else if (value != null && !type.isAssignableFrom(value.getClass())) {
                            throw new IllegalStateException("the metadata key \"" + key + "\" must have an assignable type with \"" + type.getName() + "\" from plugin: " + plugin.getReference().getName());
                        }
                    }
                }

                // Start loading plugin
                state = callAcceptHandlers(plugin);

                if (state == SUPPRESSED) {
                    builders.remove(builder);
                } else if (state == null) {
                    // Start plugin
                    try {
                        plugin.start();
                        builders.remove(builder);
                    } catch (@NotNull PluginInitializeException e) {
                        throw e;
                    } catch (@NotNull Throwable throwable) {
                        throw new PluginInitializeException(plugin.getReference(), "cannot initialize plugin correctly", throwable);
                    }
                }
            }

            callEveryoneAgain(builders);

            iterator = organizeBuilders(builders).iterator();
        }

        // Finish
        this.plugins.addAll(organizePlugins(plugins));
    }

    // Utilities

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
            @NotNull Set<ClassLoader> classLoaders = new LinkedHashSet<>(finder.getClassLoaders());
            classLoaders.add(getCallerClass().getClassLoader() != null ? getCallerClass().getClassLoader() : Thread.currentThread().getContextClassLoader());

            for (@NotNull ClassLoader classLoader : classLoaders) {
                @Nullable Class<?> reference = data.loadIfPlugin(classLoader, finder);

                if (reference != null) {
                    references.add(reference);
                    break;
                }
            }
        };

        // Collect all references
        Classes.consumeAllClasses(consumer);

        // Finish
        return references;
    }
    private @NotNull Class<?> getCallerClass() {
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

    private @Nullable HandlerState callAcceptHandlers(@NotNull Builder builder) {
        // Variables
        @NotNull Class<?> reference = builder.getReference();
        @Nullable HandlerState state = null;

        // Call category handlers
        for (@NotNull Category annotation : reference.getAnnotationsByType(Category.class)) {
            @NotNull String name = annotation.value();
            @Nullable PluginCategory category = getFactory().getCategory(annotation.value(), false).orElse(null);

            if (category != null) {
                if (alreadyAcceptedHandler(builder, category)) {
                    continue;
                }

                // Add category
                builder.category(category);
                state = HandlerState.ACCEPTED;

                // Category handlers
                if (callCategoryAndCheckSuppressed(builder, category)) {
                    return SUPPRESSED;
                }
            } else {
                builder.category(annotation.value());
            }
        }

        // Call global handlers
        for (@NotNull PluginHandler handler : Plugins.getPluginFactory().getGlobalHandlers()) {
            // Category handlers
            if (alreadyAcceptedHandler(builder, handler)) {
                continue;
            } else if (!handler.accept(builder)) {
                return SUPPRESSED;
            }

            state = HandlerState.ACCEPTED;
        }

        // Don't remove it!
        return state;
    }
    private @Nullable HandlerState callAcceptHandlers(@NotNull PluginInfo plugin) {
        // Variables
        @NotNull Class<?> reference = plugin.getReference();
        @Nullable HandlerState state = null;

        // Call category handlers
        for (@NotNull PluginCategory category : plugin.getCategories()) {
            if (alreadyAcceptedHandler(plugin, category)) {
                continue;
            }

            // Add category
            state = HandlerState.ACCEPTED;

            // Category handlers
            if (callCategoryAndCheckSuppressed(plugin, category)) {
                return SUPPRESSED;
            }
        }

        // Call global handlers
        for (@NotNull PluginHandler handler : Plugins.getPluginFactory().getGlobalHandlers()) {
            // Category handlers
            if (alreadyAcceptedHandler(plugin, handler)) {
                continue;
            } else if (!handler.accept(plugin)) {
                return SUPPRESSED;
            }

            state = HandlerState.ACCEPTED;
        }

        // Don't remove it!
        return state;
    }

    // Static utilities

    private static @NotNull Set<Class<?>> organizeClasses(@NotNull Set<Class<?>> references) {
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
    public static @NotNull Set<PluginInfo> organizePlugins(@NotNull Set<PluginInfo> plugins) {
        @NotNull Map<Class<?>, PluginInfo> map = plugins.stream().collect(Collectors.toMap(PluginInfo::getReference, Function.identity()));
        @NotNull Set<Class<?>> sortedReferences = organizeClasses(map.keySet());

        @NotNull Set<PluginInfo> sorted = new LinkedHashSet<>();
        for (@NotNull Class<?> reference : sortedReferences) {
            PluginInfo info = map.get(reference);
            if (info == null) {
                throw new IllegalStateException("missing PluginInfo for reference: " + reference);
            }
            sorted.add(info);
        }

        return sorted;
    }

    private static boolean callCategoryAndCheckSuppressed(@NotNull Builder builder, @NotNull PluginCategory category) {
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
    private static boolean callCategoryAndCheckSuppressed(@NotNull PluginInfo plugin, @NotNull PluginCategory category) {
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

    public enum HandlerState {
        SUPPRESSED,
        ACCEPTED
    }

}
