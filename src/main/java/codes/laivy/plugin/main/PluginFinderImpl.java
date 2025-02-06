package codes.laivy.plugin.main;

import codes.laivy.plugin.annotation.Category;
import codes.laivy.plugin.annotation.Dependency;
import codes.laivy.plugin.annotation.Initializer;
import codes.laivy.plugin.annotation.Plugin;
import codes.laivy.plugin.factory.PluginFactory.PluginFinder;
import codes.laivy.plugin.info.PluginInfo;
import codes.laivy.plugin.info.PluginInfo.State;
import codes.laivy.plugin.initializer.PluginInitializer;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

final class PluginFinderImpl implements PluginFinder {

    // Object

    private final @NotNull Set<ClassLoader> classLoaders = new HashSet<>();
    private final @NotNull Set<String> categories = new HashSet<>();
    private final @NotNull Map<String, Boolean> packages = new HashMap<>();
    private final @NotNull Set<Class<? extends PluginInitializer>> initializers = new HashSet<>();
    private final @NotNull Set<String> names = new HashSet<>();
    private final @NotNull Set<String> descriptions = new HashSet<>();

    // todo: should be a PluginInfo
    private final @NotNull Set<Class<?>> dependencies = new HashSet<>();
    // todo: should be a PluginInfo
    private final @NotNull Set<Class<?>> dependants = new HashSet<>();

    private final @NotNull Set<Object> instances = new HashSet<>();
    private final @NotNull Set<State> states = new HashSet<>();

    public PluginFinderImpl() {
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
            this.packages.put(packge.getName(), false);
        }

        return this;
    }
    @Override
    public @NotNull PluginFinder packages(@NotNull String @NotNull ... packages) {
        this.packages.clear();

        for (@NotNull String packge : packages) {
            this.packages.put(packge, false);
        }

        return this;
    }

    @Override
    public @NotNull PluginFinder addPackage(@NotNull String packge) {
        packages.put(packge, false);
        return this;
    }
    @Override
    public @NotNull PluginFinder addPackage(@NotNull String packge, boolean recursive) {
        packages.put(packge, recursive);
        return this;
    }
    @Override
    public @NotNull PluginFinder addPackage(@NotNull Package packge) {
        packages.put(packge.getName(), false);
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
    public @NotNull PluginFinder dependants(@NotNull Class<?> @NotNull ... dependants) {
        this.dependants.clear();
        this.dependants.addAll(Arrays.asList(dependants));

        return this;
    }

    @Override
    public @NotNull PluginFinder addDependency(@NotNull Class<?> dependency) {
        dependencies.add(dependency);
        return this;
    }
    @Override
    public @NotNull PluginFinder addDependant(@NotNull Class<?> dependant) {
        dependants.add(dependant);
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
                                    if (descriptor.contains(Plugin.class.getName().replace('.', '/'))) {
                                        plugin.set(true);

                                        return new AnnotationVisitor(Opcodes.ASM9) {
                                            @Override
                                            public void visit(@NotNull String name, @NotNull Object value) {
                                                if (!names.isEmpty() && name.equals("name") && !names.contains(value.toString())) {
                                                    valid.set(false);
                                                } else if (!descriptions.isEmpty() && name.equals("description") && !descriptions.contains(value.toString())) {
                                                    valid.set(false);
                                                }
                                            }
                                        };
                                    } else if (descriptor.contains(Category.class.getName().replace('.', '/'))) {
                                        return new AnnotationVisitor(Opcodes.ASM9) {
                                            @Override
                                            public void visit(@NotNull String name, @NotNull Object value) {
                                                if (!categories.isEmpty() && name.equals("name") && !categories.contains(value.toString())) {
                                                    valid.set(false);
                                                }
                                            }
                                        };
                                    } else if (descriptor.contains(Initializer.class.getName().replace('.', '/'))) {
                                        return new AnnotationVisitor(Opcodes.ASM9) {
                                            @Override
                                            public void visit(@NotNull String name, @NotNull Object value) {
                                                //noinspection unchecked
                                                if (!initializers.isEmpty() && name.equals("type") && !initializers.contains((Class<? extends PluginInitializer>) value)) {
                                                    valid.set(false);
                                                }
                                            }
                                        };
                                    } else if (descriptor.contains(Dependency.class.getName().replace('.', '/'))) {
                                        return new AnnotationVisitor(Opcodes.ASM9) {
                                            @Override
                                            public void visit(@NotNull String name, @NotNull Object value) {
                                                if (!dependencies.isEmpty() && name.equals("type") && !dependencies.contains((Class<?>) value)) {
                                                    valid.set(false);
                                                }
                                            }
                                        };
                                    }
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
    public @NotNull PluginInfo @NotNull [] load() {
        return new PluginInfo[0];
    }

    // Static initializers

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

}
