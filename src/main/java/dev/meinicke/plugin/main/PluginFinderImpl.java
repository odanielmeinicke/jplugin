package dev.meinicke.plugin.main;

import dev.meinicke.plugin.PluginInfo;
import dev.meinicke.plugin.PluginInfo.State;
import dev.meinicke.plugin.annotation.*;
import dev.meinicke.plugin.category.PluginCategory;
import dev.meinicke.plugin.exception.PluginInitializeException;
import dev.meinicke.plugin.factory.PluginFinder;
import dev.meinicke.plugin.initializer.ConstructorPluginInitializer;
import dev.meinicke.plugin.initializer.PluginInitializer;
import dev.meinicke.plugin.metadata.Metadata;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.stream.Collectors;

final class PluginFinderImpl implements PluginFinder {

    // Object

    private final @NotNull PluginFactoryImpl factory;

    private final @NotNull Set<ClassLoader> classLoaders = new HashSet<>();
    private final @NotNull Set<String> categories = new HashSet<>();
    private final @NotNull Set<Class<? extends PluginInitializer>> initializers = new HashSet<>();
    private final @NotNull Set<String> names = new HashSet<>();
    private final @NotNull Set<String> descriptions = new HashSet<>();
    private final @NotNull Set<Class<?>> dependencies = new HashSet<>();

    private final @NotNull Map<String, Boolean> packages = new HashMap<>();
    private final @NotNull Set<Class<?>> dependants = new HashSet<>();
    private final @NotNull Set<Object> instances = new HashSet<>();
    private final @NotNull Set<State> states = new HashSet<>();

    private final @NotNull Map<String, Object> attributes = new HashMap<>();
    private final @NotNull Map<String, Class<?>> metadataTypes = new HashMap<>();

    private final @NotNull Metadata metadata = new Metadata();

    private volatile boolean shutdownHook = true;

    private final @NotNull PluginLoader loader = new PluginLoader(this);

    public PluginFinderImpl(@NotNull PluginFactoryImpl factory) {
        this.factory = factory;
    }

    // Getters

    @Override
    public @NotNull PluginFactoryImpl getFactory() {
        return factory;
    }

    public @NotNull Set<ClassLoader> getClassLoaders() {
        return classLoaders;
    }
    public @NotNull Set<String> getCategories() {
        return categories;
    }
    public @NotNull Set<Class<? extends PluginInitializer>> getInitializers() {
        return initializers;
    }

    public @NotNull Set<String> getNames() {
        return names;
    }
    public @NotNull Set<String> getDescriptions() {
        return descriptions;
    }

    public @NotNull Set<Class<?>> getDependencies() {
        return dependencies;
    }
    public @NotNull Map<String, Boolean> getPackages() {
        return packages;
    }

    public boolean hasShutdownHook() {
        return shutdownHook;
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

    @Override
    public @NotNull Metadata getMetadata() {
        return metadata;
    }

    @Override
    public @NotNull PluginFinder addRequireMetadata(@NotNull String key) {
        metadataTypes.put(key.toLowerCase(), null);
        return this;
    }
    @Override
    public @NotNull PluginFinder addRequireMetadata(@NotNull String key, @NotNull Class<?> type) {
        metadataTypes.put(key.toLowerCase(), type);
        return this;
    }

    @Override
    public @NotNull PluginFinder addAttribute(@NotNull String key) {
        attributes.put(key.toLowerCase(), null);
        return this;
    }

    @Override
    public @NotNull PluginFinder addAttribute(@NotNull String key, @NotNull String value) {
        attributes.put(key.toLowerCase(), value);
        return this;
    }

    @Override
    public @NotNull PluginFinder addAttribute(@NotNull String key, @NotNull Class<?> value) {
        attributes.put(key.toLowerCase(), value);
        return this;
    }

    @Override
    public @NotNull PluginFinder addAttribute(@NotNull String key, int value) {
        attributes.put(key.toLowerCase(), value);
        return this;
    }

    @Override
    public @NotNull PluginFinder addAttribute(@NotNull String key, long value) {
        attributes.put(key.toLowerCase(), value);
        return this;
    }

    @Override
    public @NotNull PluginFinder addAttribute(@NotNull String key, float value) {
        attributes.put(key.toLowerCase(), value);
        return this;
    }

    @Override
    public @NotNull PluginFinder addAttribute(@NotNull String key, double value) {
        attributes.put(key.toLowerCase(), value);
        return this;
    }

    @Override
    public @NotNull PluginFinder addAttribute(@NotNull String key, boolean value) {
        attributes.put(key.toLowerCase(), value);
        return this;
    }

    @Override
    public @NotNull PluginFinder addAttribute(@NotNull String key, byte value) {
        attributes.put(key.toLowerCase(), value);
        return this;
    }

    @Override
    public @NotNull PluginFinder addAttribute(@NotNull String key, short value) {
        attributes.put(key.toLowerCase(), value);
        return this;
    }

    @Override
    public @NotNull PluginFinder addAttribute(@NotNull String key, char value) {
        attributes.put(key.toLowerCase(), value);
        return this;
    }

    @Override
    public @NotNull PluginFinder addAttribute(@NotNull String key, @NotNull String... values) {
        attributes.put(key.toLowerCase(), values);
        return this;
    }

    @Override
    public @NotNull PluginFinder addAttribute(@NotNull String key, @NotNull Class<?>... values) {
        attributes.put(key.toLowerCase(), values);
        return this;
    }

    @Override
    public @NotNull PluginFinder addAttribute(@NotNull String key, int... values) {
        attributes.put(key.toLowerCase(), values);
        return this;
    }

    @Override
    public @NotNull PluginFinder addAttribute(@NotNull String key, long... values) {
        attributes.put(key.toLowerCase(), values);
        return this;
    }

    @Override
    public @NotNull PluginFinder addAttribute(@NotNull String key, float... values) {
        attributes.put(key.toLowerCase(), values);
        return this;
    }

    @Override
    public @NotNull PluginFinder addAttribute(@NotNull String key, double... values) {
        attributes.put(key.toLowerCase(), values);
        return this;
    }

    @Override
    public @NotNull PluginFinder addAttribute(@NotNull String key, boolean... values) {
        attributes.put(key.toLowerCase(), values);
        return this;
    }

    @Override
    public @NotNull PluginFinder addAttribute(@NotNull String key, byte... values) {
        attributes.put(key.toLowerCase(), values);
        return this;
    }

    @Override
    public @NotNull PluginFinder addAttribute(@NotNull String key, short... values) {
        attributes.put(key.toLowerCase(), values);
        return this;
    }

    @Override
    public @NotNull PluginFinder addAttribute(@NotNull String key, char... values) {
        attributes.put(key.toLowerCase(), values);
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

        // Check metadata
        for (@NotNull Entry<String, Class<?>> entry : metadataTypes.entrySet()) {
            @Nullable RequireMetadata annotation = Arrays.stream(reference.getAnnotationsByType(RequireMetadata.class)).filter(a -> a.key().equalsIgnoreCase(entry.getKey())).findFirst().orElse(null);

            if (annotation == null) {
                return false;
            } else if (entry.getValue() != null && !entry.getValue().isAssignableFrom(annotation.type())) {
                return false;
            }
        }

        // Check attribute
        for (@NotNull Entry<String, Object> entry : attributes.entrySet()) {
            @Nullable Attribute annotation = Arrays.stream(reference.getAnnotationsByType(Attribute.class)).filter(a -> a.key().equalsIgnoreCase(entry.getKey())).findFirst().orElse(null);

            if (annotation == null) {
                return false;
            } else if (entry.getValue() != null && !entry.getValue().getClass().isAssignableFrom(annotation.type())) {
                return false;
            }
        }

        return true;
    }
    @Override
    public @NotNull PluginInfo @NotNull [] plugins() {
        // Variables
        @NotNull Set<PluginInfo> plugins = new HashSet<>();

        // Detect plugins
        for (@NotNull PluginInfo plugin : getFactory()) {
            if (matches(plugin)) {
                plugins.add(plugin);
            }
        }

        // Finish
        return PluginLoader.organizePlugins(plugins).toArray(new PluginInfo[0]);
    }

    @Override
    public @NotNull Class<?> @NotNull [] classes() throws IOException {
        return loader.getClasses().toArray(new Class[0]);
    }

    // Load

    @Override
    public @NotNull PluginInfo @NotNull [] load(@NotNull Predicate<Class<?>> predicate) throws PluginInitializeException, IOException {
        return loader.plugins(predicate).toArray(new PluginInfo[0]);
    }

    // Utilities

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean checkPackageWithin(@NotNull String reference) {
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

}
