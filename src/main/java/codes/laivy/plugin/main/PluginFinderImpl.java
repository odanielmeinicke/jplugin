package codes.laivy.plugin.main;

import codes.laivy.plugin.PluginInfo;
import codes.laivy.plugin.PluginInfo.Builder;
import codes.laivy.plugin.PluginInfo.State;
import codes.laivy.plugin.annotation.Category;
import codes.laivy.plugin.annotation.Dependency;
import codes.laivy.plugin.annotation.Initializer;
import codes.laivy.plugin.annotation.Plugin;
import codes.laivy.plugin.category.PluginCategory;
import codes.laivy.plugin.exception.InvalidPluginException;
import codes.laivy.plugin.exception.PluginInitializeException;
import codes.laivy.plugin.exception.PluginInterruptException;
import codes.laivy.plugin.factory.PluginFactory;
import codes.laivy.plugin.factory.PluginFinder;
import codes.laivy.plugin.factory.handlers.PluginHandler;
import codes.laivy.plugin.initializer.ConstructorPluginInitializer;
import codes.laivy.plugin.initializer.PluginInitializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

final class PluginFinderImpl implements PluginFinder {

    // Object

    private final @NotNull PluginFactoryImpl factory;

    final @NotNull Set<ClassLoader> classLoaders = new HashSet<>();
    final @NotNull Set<String> categories = new HashSet<>();
    final @NotNull Set<Class<? extends PluginInitializer>> initializers = new HashSet<>();
    final @NotNull Set<String> names = new HashSet<>();
    final @NotNull Set<String> descriptions = new HashSet<>();
    final @NotNull Set<Class<?>> dependencies = new HashSet<>();

    private final @NotNull Map<String, Boolean> packages = new HashMap<>();
    private final @NotNull Set<Class<?>> dependants = new HashSet<>();
    private final @NotNull Set<Object> instances = new HashSet<>();
    private final @NotNull Set<State> states = new HashSet<>();

    private final @NotNull Class<?> caller;

    private volatile boolean shutdownHook = true;

    public PluginFinderImpl(@NotNull PluginFactoryImpl factory) {
        this.factory = factory;

        // Retrieve caller class
        @NotNull StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

        if (stackTrace.length > 3) {
            try {
                this.caller = Class.forName(stackTrace[3].getClassName());
            } catch (@NotNull ClassNotFoundException e) {
                throw new RuntimeException("cannot retrieve caller class", e);
            }
        } else {
            throw new RuntimeException("invalid stack traces to retrieve caller class");
        }
    }

    // Class Loaders

    @Override
    public @NotNull PluginFinder classLoaders(@NotNull ClassLoader @NotNull ... loaders) {
        classLoaders.clear();
        classLoaders.addAll(Arrays.asList(loaders));

        return this;
    }

    @Override
    public @NotNull PluginFinder addClassLoader(@NotNull ClassLoader loader) {
        classLoaders.add(loader);
        return this;
    }

    // Categories

    @Override
    public @NotNull PluginFinder categories(@NotNull PluginCategory @NotNull ... categories) {
        this.categories.clear();
        this.categories.addAll(Arrays.stream(categories).map(PluginCategory::getName).collect(Collectors.toList()));

        return this;
    }
    @Override
    public @NotNull PluginFinder categories(@NotNull String @NotNull ... categories) {
        this.categories.clear();
        this.categories.addAll(Arrays.asList(categories));

        return this;
    }

    @Override
    public @NotNull PluginFinder addCategory(@NotNull PluginCategory category) {
        categories.add(category.getName());
        return this;
    }
    @Override
    public @NotNull PluginFinder addCategory(@NotNull String category) {
        categories.add(category);
        return this;
    }

    // Packages

    @Override
    public @NotNull PluginFinder packages(@NotNull Package @NotNull ... packages) {
        this.packages.clear();

        for (@NotNull Package packge : packages) {
            this.packages.put(packge.getName(), true);
        }

        return this;
    }
    @Override
    public @NotNull PluginFinder packages(@NotNull String @NotNull ... packages) {
        this.packages.clear();

        for (@NotNull String packge : packages) {
            this.packages.put(packge, true);
        }

        return this;
    }

    @Override
    public @NotNull PluginFinder addPackage(@NotNull String packge) {
        packages.put(packge, true);
        return this;
    }
    @Override
    public @NotNull PluginFinder addPackage(@NotNull String packge, boolean recursive) {
        packages.put(packge, recursive);
        return this;
    }
    @Override
    public @NotNull PluginFinder addPackage(@NotNull Package packge) {
        packages.put(packge.getName(), true);
        return this;
    }
    @Override
    public @NotNull PluginFinder addPackage(@NotNull Package packge, boolean recursive) {
        packages.put(packge.getName(), recursive);
        return this;
    }

    // Initializers

    @Override
    public @NotNull PluginFinder initializers(@NotNull Class<? extends PluginInitializer> @NotNull [] initializers) {
        this.initializers.clear();
        this.initializers.addAll(Arrays.asList(initializers));

        return this;
    }
    @Override
    public @NotNull PluginFinder initializer(@NotNull Class<? extends PluginInitializer> initializer) {
        initializers.clear();
        initializers.add(initializer);

        return this;
    }

    @Override
    public @NotNull PluginFinder addInitializer(@NotNull Class<? extends PluginInitializer> initializer) {
        initializers.add(initializer);
        return this;
    }

    // Names and descriptions

    @Override
    public @NotNull PluginFinder names(@NotNull String @NotNull ... names) {
        this.names.clear();
        this.names.addAll(Arrays.asList(names));

        return this;
    }

    @Override
    public @NotNull PluginFinder addName(@NotNull String name) {
        names.add(name);
        return this;
    }

    @Override
    public @NotNull PluginFinder descriptions(@NotNull String @NotNull ... descriptions) {
        this.descriptions.clear();
        this.descriptions.addAll(Arrays.asList(descriptions));

        return this;
    }

    @Override
    public @NotNull PluginFinder addDescription(@NotNull String description) {
        descriptions.add(description);
        return this;
    }

    // Dependencies and dependants

    @Override
    public @NotNull PluginFinder dependencies(@NotNull Class<?> @NotNull ... dependencies) {
        this.dependencies.clear();
        this.dependencies.addAll(Arrays.asList(dependencies));

        return this;
    }

    @Override
    public @NotNull PluginFinder dependencies(@NotNull PluginInfo @NotNull ... dependencies) {
        this.dependencies.clear();
        this.dependencies.addAll(Arrays.stream(dependencies).map(PluginInfo::getReference).collect(Collectors.toSet()));

        return this;
    }

    @Override
    public @NotNull PluginFinder dependants(@NotNull Class<?> @NotNull ... dependants) {
        this.dependants.clear();
        this.dependants.addAll(Arrays.asList(dependants));

        return this;
    }

    @Override
    public @NotNull PluginFinder dependants(@NotNull PluginInfo @NotNull ... dependants) {
        this.dependants.clear();
        this.dependants.addAll(Arrays.stream(dependants).map(PluginInfo::getReference).collect(Collectors.toSet()));

        return this;
    }

    @Override
    public @NotNull PluginFinder addDependency(@NotNull Class<?> dependency) {
        dependencies.add(dependency);
        return this;
    }

    @Override
    public @NotNull PluginFinder addDependency(@NotNull PluginInfo dependency) {
        dependencies.add(dependency.getReference());
        return this;
    }

    @Override
    public @NotNull PluginFinder addDependant(@NotNull Class<?> dependant) {
        dependants.add(dependant);
        return this;
    }

    @Override
    public @NotNull PluginFinder addDependant(@NotNull PluginInfo dependant) {
        dependants.add(dependant.getReference());
        return this;
    }

    // Instances

    @Override
    public @NotNull PluginFinder instances(@NotNull Object @NotNull ... instances) {
        this.instances.clear();
        this.instances.addAll(Arrays.asList(instances));

        return this;
    }

    @Override
    public @NotNull PluginFinder addInstance(@NotNull Object instance) {
        instances.add(instance);
        return this;
    }

    // States

    @Override
    public @NotNull PluginFinder states(@NotNull State @NotNull ... states) {
        this.states.clear();
        this.states.addAll(Arrays.asList(states));

        return this;
    }

    @Override
    public @NotNull PluginFinder addState(@NotNull State state) {
        states.add(state);
        return this;
    }

    // Others

    @Override
    public @NotNull PluginFinder setShutdownHook(boolean shutdownHook) {
        this.shutdownHook = shutdownHook;
        return this;
    }

    // Query

    @SuppressWarnings("RedundantIfStatement")
    @Override
    public boolean matches(@NotNull PluginInfo plugin) {
        if (!classLoaders.isEmpty() && classLoaders.contains(plugin.getReference().getClassLoader())) {
            return false;
        } else if (!categories.isEmpty() && categories.containsAll(plugin.getCategories().stream().map(PluginCategory::getName).collect(Collectors.toList()))) {
            return false;
        } else if (!checkPackageWithin(plugin.getReference().getPackage().getName())) {
            return false;
        } else if (!initializers.isEmpty() && !initializers.contains(plugin.getInitializer())) {
            return false;
        } else if (!names.isEmpty() && !names.contains(plugin.getName())) {
            return false;
        } else if (!descriptions.isEmpty() && !descriptions.contains(plugin.getDescription())) {
            return false;
        } else if (!dependencies.isEmpty() && !dependencies.containsAll(plugin.getDependencies().stream().map(PluginInfo::getReference).collect(Collectors.toList()))) {
            return false;
        } else if (!dependants.isEmpty() && !dependants.containsAll(plugin.getDependants().stream().map(PluginInfo::getReference).collect(Collectors.toList()))) {
            return false;
        } else if (!instances.isEmpty() && !instances.contains(plugin.getInstance())) {
            return false;
        } else if (!states.isEmpty() && !states.contains(plugin.getState())) {
            return false;
        }

        return true;
    }
    @SuppressWarnings("RedundantIfStatement")
    @Override
    public boolean matches(@NotNull Class<?> reference) {
        if (!reference.isAnnotationPresent(Plugin.class)) {
            return false;
        }

        @NotNull ClassLoader classLoader = reference.getClassLoader();
        @NotNull Set<String> categories = Arrays.stream(reference.getAnnotationsByType(Category.class)).map(Category::value).map(String::toLowerCase).collect(Collectors.toSet());
        @NotNull String packge = reference.getPackage().getName();
        @NotNull Class<? extends PluginInitializer> initializer = reference.isAnnotationPresent(Initializer.class) ? reference.getAnnotation(Initializer.class).type() : ConstructorPluginInitializer.class;
        @NotNull String name = reference.getAnnotation(Plugin.class).name();
        @NotNull String description = reference.getAnnotation(Plugin.class).description();
        @NotNull Set<Class<?>> dependencies = Arrays.stream(reference.getAnnotationsByType(Dependency.class)).map(Dependency::type).collect(Collectors.toSet());

        if (!classLoaders.isEmpty() && classLoaders.contains(classLoader)) {
            return false;
        } else if (!this.categories.isEmpty() && this.categories.stream().map(String::toLowerCase).collect(Collectors.toSet()).containsAll(categories)) {
            return false;
        } else if (!checkPackageWithin(packge)) {
            return false;
        } else if (!initializers.isEmpty() && !initializers.contains(initializer)) {
            return false;
        } else if (!names.isEmpty() && !names.contains(name)) {
            return false;
        } else if (!descriptions.isEmpty() && !descriptions.contains(description)) {
            return false;
        } else if (!this.dependencies.isEmpty() && !this.dependencies.containsAll(dependencies)) {
            return false;
        }

        return true;
    }

    @Override
    public @NotNull PluginInfo @NotNull [] plugins() {
        // Variables
        @NotNull Set<PluginInfo> plugins = new HashSet<>();

        // Detect plugins
        for (@NotNull PluginInfo plugin : Plugins.getFactory()) {
            if (matches(plugin)) {
                plugins.add(plugin);
            }
        }

        // Finish
        return plugins.toArray(new PluginInfo[0]);
    }

    // todo: this finds the inner classes plugins also?
    @Override
    public @NotNull Class<?> @NotNull [] classes() throws IOException {
        // Variables
        @NotNull Set<@NotNull Class<?>> references = new HashSet<>();

        // Consumer
        @NotNull Consumer<ClassData> consumer = new Consumer<ClassData>() {
            @Override
            public void accept(@NotNull ClassData data) {
                // Variables
                @NotNull String name = data.getName();
                @NotNull ClassLoader classLoader = data.getClassLoader();
                @NotNull InputStream inputStream = data.getInputStream();

                // Verify package
                @NotNull String packge = name.contains(".") ? name.substring(0, name.lastIndexOf('.')) : "";
                if (!checkPackageWithin(packge)) return;

                // Load if it's a plugin
                @Nullable Class<?> reference = data.loadIfPlugin(PluginFinderImpl.this);
                if (reference != null) references.add(reference);
            }
        };

        // Collect all references
        if (classLoaders.isEmpty()) {
            @NotNull URL url = caller.getProtectionDomain().getCodeSource().getLocation();
            Classes.getAllTypeClassesWithVisitor(url, caller.getClassLoader(), consumer);
        } else {
            Classes.getAllTypeClassesWithVisitor(classLoaders, consumer);
        }

        // Load references
        return references.toArray(new Class[0]);
    }

    // Load

    @Override
    public @NotNull PluginInfo @NotNull [] load(@NotNull Predicate<Class<?>> predicate) throws PluginInitializeException, IOException {
        // Variables
        @NotNull Map<Class<?>, Builder> builders = new LinkedHashMap<>();
        @NotNull Map<Class<?>, PluginInfo> plugins = new LinkedHashMap<>();
        @NotNull Map<Builder, Collection<PluginCategory>> categories = new HashMap<>();

        // Create the builder instances
        // This create the instance without checking for categories and dependencies.

        main:
        for (@NotNull Class<?> reference : classes()) {
            // Verifications
            {
                // Check if predicate validates it
                if (!predicate.test(reference)) {
                    continue;
                } else if (factory.plugins.containsKey(reference) && !factory.plugins.get(reference).getState().isIdle()) {
                    continue;
                }

                // Check if it's an inner and non-class
                if (reference.getEnclosingClass() != null && !Modifier.isStatic(reference.getModifiers())) {
                    throw new InvalidPluginException(reference, "a non-static inner class cannot be a plugin, the class should be at least static");
                }
            }

            // Retrieve plugin initializer
            @NotNull PluginInitializer initializer = getInitializer(reference);

            // Dependencies
            @NotNull Set<Class<?>> dependencies = getDependencies(reference);

            // Name
            @Nullable String name = reference.getAnnotation(Plugin.class).name();
            if (name.isEmpty()) name = null;

            // Description
            @Nullable String description = reference.getAnnotation(Plugin.class).description();
            if (description.isEmpty()) description = null;

            // Create instance
            @NotNull Builder builder = initializer.create(reference, name, description, dependencies.toArray(new Class[0]), new String[0]);

            // Add to the builder only the categories that actually exists (for now)
            categories.putIfAbsent(builder, new LinkedList<>());

            for (@NotNull Category annotation : reference.getAnnotationsByType(Category.class)) {
                @Nullable PluginCategory category = factory.getCategory(annotation.value(), false).orElse(null);

                if (category != null) {
                    // Add category to builder
                    builder.category(category);
                    categories.get(builder).add(category);

                    // Category handlers
                    if (callCategory(builder, category)) {
                        continue main;
                    }
                }
            }

            // Register it
            builders.put(reference, builder);
        }

        // Shutdown hook
        @NotNull Set<PluginInfo> loadedPlugins = new LinkedHashSet<>();

        if (shutdownHook) {
            @NotNull ShutdownHook hook = new ShutdownHook(loadedPlugins);
            Runtime.getRuntime().addShutdownHook(hook);
        }

        // Organize by dependencies order
        @NotNull Iterator<Builder> iterator = organize(builders.values()).iterator();
        @NotNull Set<Builder> done = new HashSet<>();
        @NotNull Set<Builder> refreshed = new HashSet<>();

        main:
        while (iterator.hasNext()) {
            // Variables
            @NotNull Builder builder = iterator.next();
            @NotNull Class<?> reference = builder.getReference();
            categories.put(builder, new LinkedList<>());

            // Dependencies
            for (@NotNull Class<?> dependency : builder.getDependencies()) {
                if (!builders.containsKey(dependency) && !factory.plugins.containsKey(dependency)) {
                    if (dependency.isAnnotationPresent(Plugin.class)) {
                        throw new InvalidPluginException(reference, "the plugin '" + reference.getName() + "' depends on '" + dependency.getName() + "' that isn't loaded.");
                    } else {
                        throw new InvalidPluginException(reference, "the plugin '" + reference.getName() + "' cannot have a dependency on '" + dependency.getName() + "' because it's not a plugin");
                    }
                }
            }

            if (!refreshed.contains(builder)) {
                // Categories
                for (@NotNull Category annotation : reference.getAnnotationsByType(Category.class)) {
                    @Nullable PluginCategory category = factory.getCategory(annotation.value(), false).orElse(null);

                    if (categories.get(builder).contains(category)) {
                        continue;
                    }

                    if (category != null) {
                        builder.category(category);
                        categories.get(builder).add(category);

                        // Category handlers
                        if (callCategory(builder, category)) {
                            continue main;
                        }
                    } else {
                        builder.category(annotation.value());
                    }
                }

                // Call global handlers
                for (@NotNull PluginHandler handler : factory.getGlobalHandlers()) {
                    if (!handler.accept(builder)) {
                        continue main;
                    }
                }

                refreshed.add(builder);

                @NotNull Set<Builder> next = new LinkedHashSet<>(builders.values());
                next.removeAll(done);

                iterator = organize(next).iterator();

                continue;
            }

            // Build
            @NotNull PluginInfo plugin;

            try {
                plugin = builder.build();
            } catch (@NotNull Throwable e) {
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
                for (@NotNull PluginHandler handler : factory.getGlobalHandlers()) {
                    if (!handler.accept(plugin)) {
                        continue main;
                    }
                }
            }

            // Register it
            factory.plugins.put(reference, plugin);
            plugins.put(reference, plugin);

            try {
                plugin.start();

                if (plugin.isAutoClose()) {
                    loadedPlugins.add(plugin);
                }
            } catch (@NotNull PluginInitializeException e) {
                throw e;
            } catch (@NotNull Throwable throwable) {
                throw new PluginInitializeException(plugin.getReference(), "cannot initialize plugin correctly", throwable);
            }

            // Refresh iterator
            done.add(builder);

            @NotNull Set<Builder> next = new LinkedHashSet<>(builders.values());
            next.removeAll(done);

            iterator = organize(next).iterator();
        }

        // Add dependencies
        for (@NotNull PluginInfo plugin : plugins.values()) {
            @NotNull List<@NotNull PluginInfo> dependants = plugins.values().stream().filter(target -> target.getDependencies().contains(plugin)).collect(Collectors.toList());
            plugin.getDependants().addAll(dependants);
        }

        // Finish
        return plugins.values().toArray(new PluginInfo[0]);
    }

    @Override
    public @NotNull PluginFactory getFactory() {
        return factory;
    }


    // Utilities

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean checkPackageWithin(@NotNull String reference) {
        if (packages.isEmpty()) {
            return true;
        }

        boolean any = false;

        for (@NotNull Entry<String, Boolean> entry : packages.entrySet()) {
            // Variables
            @NotNull String required = entry.getKey();
            boolean recursive = entry.getValue();

            // Check any
            if (recursive) any = reference.startsWith(required);
            else any = reference.equals(required);

            // Break if founded
            if (any) {
                break;
            }
        }

        return any;
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
    private static @NotNull Set<Builder> organize(@NotNull Collection<@NotNull Builder> plugins) {
        @NotNull Set<Builder> sorted = new LinkedHashSet<>();
        @NotNull List<Builder> remaining = new LinkedList<>(plugins);
        @NotNull Map<Class<?>, Builder> builderByReference = plugins.stream()
                .collect(Collectors.toMap(Builder::getReference, Function.identity()));

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

        return sorted;
    }

    private static @NotNull PluginInitializer getInitializer(@NotNull Class<?> reference) {
        // Plugin loader class
        @NotNull Class<? extends PluginInitializer> loaderClass = ConstructorPluginInitializer.class;
        if (reference.isAnnotationPresent(Initializer.class)) {
            loaderClass = reference.getAnnotation(Initializer.class).type();
        }

        try {
            // Constructor
            @NotNull Constructor<? extends PluginInitializer> constructor = loaderClass.getDeclaredConstructor();
            constructor.setAccessible(true);

            return constructor.newInstance();
        } catch (@NotNull InvocationTargetException e) {
            throw new RuntimeException("cannot execute plugin loader's constructor: " + loaderClass, e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("cannot find plugin loader's empty declared constructor: " + loaderClass, e);
        } catch (InstantiationException e) {
            throw new RuntimeException("cannot instantiate plugin loader: " + loaderClass, e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("cannot access plugin loader's constructor: " + loaderClass, e);
        }
    }
    private static @NotNull Set<Class<?>> getDependencies(@NotNull Class<?> reference) {
        @NotNull Set<Class<?>> dependencies = new LinkedHashSet<>();

        for (@NotNull Dependency annotation : reference.getAnnotationsByType(Dependency.class)) {
            // Check issues
            if (annotation.type() == reference) {
                throw new InvalidPluginException(reference, "the plugin cannot have a dependency on itself");
            } else for (@NotNull Dependency a : annotation.type().getAnnotationsByType(Dependency.class)) {
                @NotNull Class<?> dependency = a.type();

                if (dependency == reference) {
                    throw new InvalidPluginException(reference, "cyclic dependency between '" + reference.getName() + "' and '" + dependency.getName() + "'.");
                }
            }

            // Variables
            @NotNull Class<?> dependency = annotation.type();

            // Generate instance and register it
            dependencies.add(dependency);
        }

        // Finish
        return dependencies;
    }

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
            } catch (@NotNull Throwable throwable) {
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
            } catch (@NotNull Throwable throwable) {
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
                } catch (@NotNull PluginInterruptException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

}
