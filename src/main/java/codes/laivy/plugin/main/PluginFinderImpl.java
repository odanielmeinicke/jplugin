package codes.laivy.plugin.main;

import codes.laivy.plugin.annotation.Category;
import codes.laivy.plugin.annotation.Dependency;
import codes.laivy.plugin.annotation.Initializer;
import codes.laivy.plugin.annotation.Plugin;
import codes.laivy.plugin.category.PluginHandler;
import codes.laivy.plugin.exception.InvalidPluginException;
import codes.laivy.plugin.exception.PluginInitializeException;
import codes.laivy.plugin.factory.PluginFinder;
import codes.laivy.plugin.PluginInfo;
import codes.laivy.plugin.PluginInfo.State;
import codes.laivy.plugin.initializer.MethodPluginInitializer;
import codes.laivy.plugin.initializer.PluginInitializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

final class PluginFinderImpl implements PluginFinder {

    // Object

    private final @NotNull PluginFactoryImpl factory;

    private final @NotNull Set<ClassLoader> classLoaders = new HashSet<>();
    private final @NotNull Set<String> categories = new HashSet<>();
    private final @NotNull Map<String, Boolean> packages = new HashMap<>();
    private final @NotNull Set<Class<? extends PluginInitializer>> initializers = new HashSet<>();
    private final @NotNull Set<String> names = new HashSet<>();
    private final @NotNull Set<String> descriptions = new HashSet<>();

    private final @NotNull Set<Class<?>> dependencies = new HashSet<>();
    private final @NotNull Set<Class<?>> dependants = new HashSet<>();

    private final @NotNull Set<Object> instances = new HashSet<>();
    private final @NotNull Set<State> states = new HashSet<>();

    public PluginFinderImpl(@NotNull PluginFactoryImpl factory) {
        this.factory = factory;
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
    public @NotNull PluginFinder categories(@NotNull String @NotNull ... categories) {
        this.categories.clear();
        this.categories.addAll(Arrays.asList(categories));

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

    // Query

    @SuppressWarnings("RedundantIfStatement")
    @Override
    public boolean matches(@NotNull PluginInfo plugin) {
        if (!classLoaders.isEmpty() && classLoaders.contains(plugin.getReference().getClassLoader())) {
            return false;
        } else if (!categories.isEmpty() && categories.containsAll(plugin.getCategories())) {
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
        @NotNull Set<String> categories = Arrays.stream(reference.getAnnotationsByType(Category.class)).map(Category::name).distinct().collect(Collectors.toSet());
        @NotNull String packge = reference.getPackage().getName();
        @NotNull Class<? extends PluginInitializer> initializer = reference.isAnnotationPresent(Initializer.class) ? reference.getAnnotation(Initializer.class).type() : MethodPluginInitializer.class;
        @NotNull String name = reference.getAnnotation(Plugin.class).name();
        @NotNull String description = reference.getAnnotation(Plugin.class).description();
        @NotNull Set<Class<?>> dependencies = Arrays.stream(reference.getAnnotationsByType(Dependency.class)).map(Dependency::type).collect(Collectors.toSet());

        if (!classLoaders.isEmpty() && classLoaders.contains(classLoader)) {
            return false;
        } else if (!this.categories.isEmpty() && this.categories.containsAll(categories)) {
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
    @Override
    public @NotNull Class<?> @NotNull [] classes() throws IOException {
        // Variables
        @NotNull Set<@NotNull Class<?>> references = new HashSet<>();

        // Collect all references
        Classes.getAllTypeClassesWithVisitor(new BiConsumer<String, InputStream>() {
            @Override
            public void accept(@NotNull String name, @NotNull InputStream stream) {
                @NotNull String packge = name.contains(".") ? name.substring(0, name.lastIndexOf('.')) : "";

                if (!checkPackageWithin(packge)) {
                    return;
                }

                try {
                    @NotNull Class<?> reference = Class.forName(name, false, ClassLoader.getSystemClassLoader());

                    if (reference.isAnnotationPresent(Plugin.class) && (classLoaders.isEmpty() || classLoaders.contains(reference.getClassLoader()))) {
                        references.add(reference);
                    }
                } catch (@NotNull ClassNotFoundException | @NotNull NoClassDefFoundError e) {
                    try {
                        @NotNull AtomicBoolean plugin = new AtomicBoolean(false);
                        @NotNull AtomicBoolean valid = new AtomicBoolean(true);

                        @NotNull ClassReader reader = new ClassReader(stream);
                        @NotNull ClassVisitor visitor = new ClassVisitor(Opcodes.ASM9) {
                            @Override
                            public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                                if (valid.get()) {
                                    return new AnnotationVisitor(Opcodes.ASM9) {
                                        @Override
                                        public void visit(String name, Object value) {
                                            if (descriptor.contains(Plugin.class.getName().replace('.', '/'))) {
                                                plugin.set(true);

                                                if (!names.isEmpty() && name.equals("name") && !names.contains(value.toString())) {
                                                    valid.set(false);
                                                } else if (!descriptions.isEmpty() && name.equals("description") && !descriptions.contains(value.toString())) {
                                                    valid.set(false);
                                                }
                                            } else if (descriptor.contains(Category.class.getName().replace('.', '/'))) {
                                                if (!categories.isEmpty() && name.equals("name") && !categories.contains(value.toString())) {
                                                    valid.set(false);
                                                }
                                            } else if (descriptor.contains(Initializer.class.getName().replace('.', '/'))) {
                                                //noinspection unchecked
                                                if (!initializers.isEmpty() && name.equals("type") && !initializers.contains((Class<? extends PluginInitializer>) value)) {
                                                    valid.set(false);
                                                }
                                            } else if (descriptor.contains(Dependency.class.getName().replace('.', '/'))) {
                                                if (!dependencies.isEmpty() && name.equals("type") && !dependencies.contains((Class<?>) value)) {
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

                        reader.accept(visitor, 0);

                        if (plugin.get() && valid.get()) {
                            //noinspection deprecation
                            references.add(Classes.define(reader.b));
                        }
                    } catch (@NotNull IOException ignore) {
                    }
                }
            }
        });

        // Load references
        return references.toArray(new Class[0]);
    }

    // Load

    @Override
    public @NotNull PluginInfo @NotNull [] load(@NotNull Predicate<Class<?>> predicate) throws PluginInitializeException, IOException {
        // Variables
        @NotNull Map<Class<?>, PluginInfo> plugins = new LinkedHashMap<>();

        // Create instances
        main:
        for (@NotNull Class<?> reference : organize(new HashSet<>(Arrays.asList(classes())))) {
            // Check if predicate validates it
            if (!predicate.test(reference)) {
                continue;
            }

            // Check if it's an inner and non-class
            if (reference.getEnclosingClass() != null && !Modifier.isStatic(reference.getModifiers())) {
                throw new InvalidPluginException(reference, "a non-inner class cannot be a plugin, the class should be atleast static");
            }

            // Retrieve plugin loader
            @NotNull PluginInitializer initializer;

            {
                // Plugin loader class
                @NotNull Class<? extends PluginInitializer> loaderClass = MethodPluginInitializer.class;
                if (reference.isAnnotationPresent(Initializer.class)) {
                    loaderClass = reference.getAnnotation(Initializer.class).type();
                }

                try {
                    // Constructor
                    @NotNull Constructor<? extends PluginInitializer> constructor = loaderClass.getDeclaredConstructor();
                    constructor.setAccessible(true);

                    initializer = constructor.newInstance();
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

            // Dependencies
            @NotNull Set<PluginInfo> dependencies = new LinkedHashSet<>();

            for (@NotNull Dependency annotation : reference.getAnnotationsByType(Dependency.class)) {
                @NotNull Class<?> dependency = annotation.type();

                // Check issues
                if (dependency == reference) {
                    throw new InvalidPluginException(reference, "the plugin cannot have a dependency on itself");
                }

                // Generate instance
                if (plugins.containsKey(dependency)) {
                    dependencies.add(plugins.get(dependency));
                } else {
                    throw new InvalidPluginException(reference, "there's a dependency that is not a plugin at '" + reference.getName() + "': " + dependency.getName());
                }
            }

            // Name
            @Nullable String name = reference.getAnnotation(Plugin.class).name();
            if (name.isEmpty()) name = null;

            // Description
            @Nullable String description = reference.getAnnotation(Plugin.class).description();
            if (description.isEmpty()) description = null;

            // Categories
            @NotNull Set<String> categories = new LinkedHashSet<>();
            for (@NotNull Category category : reference.getAnnotationsByType(Category.class)) {
                categories.add(category.name());
            }

            // Create instance and register it
            @NotNull PluginInfo plugin = initializer.create(reference, name, description, dependencies.toArray(new PluginInfo[0]), categories.toArray(new String[0]));

            // Call Handlers
            {
                // Category handlers
                for (@NotNull String category : categories) {
                    for (@NotNull PluginHandler handler : Plugins.getFactory().getHandlers(category)) {
                        try {
                            if (!handler.accept(plugin)) {
                                continue main;
                            }
                        } catch (@NotNull Throwable throwable) {
                            throw new RuntimeException("cannot invoke category's handler to accept '" + category + "': " + handler);
                        }
                    }
                }

                // Global handlers
                for (@NotNull PluginHandler handler : Plugins.getFactory().getHandlers()) {
                    try {
                        if (!handler.accept(plugin)) {
                            continue main;
                        }
                    } catch (@NotNull Throwable throwable) {
                        throw new RuntimeException("cannot invoke global handler to accept '" + name + "': " + handler);
                    }
                }
            }

            // Register it
            factory.plugins.put(reference, plugin);
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
        for (@NotNull PluginInfo plugin : plugins.values()) {
            @NotNull List<@NotNull PluginInfo> dependants = plugins.values().stream().filter(target -> target.getDependencies().contains(plugin)).collect(Collectors.toList());
            plugin.getDependants().addAll(dependants);
        }

        // Finish
        return plugins.values().toArray(new PluginInfo[0]);
    }

    // Static initializers

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
    private static @NotNull Set<PluginInfo> organize(@NotNull Collection<@NotNull PluginInfo> plugins) {
        @NotNull Set<PluginInfo> set = new LinkedHashSet<>();
        @NotNull Map<Class<?>, PluginInfo> map = plugins.stream().collect(Collectors.toMap(PluginInfo::getReference, Function.identity(), (a, b) -> a, LinkedHashMap::new));
        organize(map.keySet()).forEach(ref -> set.add(map.get(ref)));

        return set;
    }

}
