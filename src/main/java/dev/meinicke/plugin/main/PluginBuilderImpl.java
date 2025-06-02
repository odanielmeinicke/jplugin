package dev.meinicke.plugin.main;

import dev.meinicke.plugin.Builder;
import dev.meinicke.plugin.PluginInfo;
import dev.meinicke.plugin.annotation.Category;
import dev.meinicke.plugin.annotation.Dependency;
import dev.meinicke.plugin.annotation.Initializer;
import dev.meinicke.plugin.annotation.Priority;
import dev.meinicke.plugin.category.PluginCategory;
import dev.meinicke.plugin.context.PluginContext;
import dev.meinicke.plugin.exception.InvalidPluginException;
import dev.meinicke.plugin.factory.InitializerFactory;
import dev.meinicke.plugin.factory.PluginFactory;
import dev.meinicke.plugin.factory.handlers.Handlers;
import dev.meinicke.plugin.initializer.ConstructorPluginInitializer;
import dev.meinicke.plugin.initializer.PluginInitializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

final class PluginBuilderImpl implements Builder {

    // Object

    private final @NotNull InitializerFactory initializerFactory;
    private final @NotNull PluginFactory factory;

    private final @NotNull Class<?> reference;
    private final @NotNull PluginContext context;

    private @Nullable String name;
    private @Nullable String description;

    private @NotNull PluginInitializer initializer;

    private final @NotNull Set<String> categories = new LinkedHashSet<>();

    private final @NotNull Set<Class<?>> dependencies = new LinkedHashSet<>();
    private int priority = 0;

    private final @NotNull Handlers handlers = Handlers.create();

    PluginBuilderImpl(@NotNull InitializerFactory initializerFactory, @NotNull PluginFactory factory, @NotNull Class<?> reference, @NotNull PluginContext context, @Nullable String name, @Nullable String description) {
        // Variables
        this.initializerFactory = initializerFactory;
        this.factory = factory;

        this.reference = reference;
        this.context = context;

        this.name = name;
        this.description = description;

        // Priority
        if (reference.isAnnotationPresent(Priority.class)) {
            this.priority = reference.getAnnotation(Priority.class).value();
        }

        // Verifications
        if (context.getPluginClass() != reference) {
            throw new IllegalArgumentException("the plugin context's reference is not the same from the parameter: " + reference.getName() + " and " + context.getPluginClass().getName());
        }

        // Dependencies
        for (@NotNull Dependency annotation : reference.getAnnotationsByType(Dependency.class)) {
            // Variables
            @NotNull Class<?> dependency = annotation.type();

            // Check issues
            checkDependency(dependency);

            // Generate instance and register it
            this.dependencies.add(dependency);
        }

        // Initializer
        @NotNull Class<? extends PluginInitializer> initializerClass = ConstructorPluginInitializer.class;
        if (reference.isAnnotationPresent(Initializer.class)) {
            initializerClass = reference.getAnnotation(Initializer.class).type();
        }

        this.initializer = initializerFactory.getInitializer(initializerClass);

        // Categories
        for (@NotNull Category annotation : reference.getAnnotationsByType(Category.class)) {
            categories.add(annotation.value());
        }
    }

    // Getters

    private void checkDependency(@NotNull Class<?> reference) {
        if (reference == getReference()) {
            throw new InvalidPluginException(reference, "the plugin cannot have a dependency on itself");
        } else for (@NotNull Dependency a : reference.getAnnotationsByType(Dependency.class)) {
            @NotNull Class<?> dependency = a.type();

            if (dependency == reference) {
                throw new InvalidPluginException(reference, "cyclic dependency between '" + reference.getName() + "' and '" + dependency.getName() + "'.");
            }
        }
    }

    @Override
    public @NotNull InitializerFactory getInitializerFactory() {
        return initializerFactory;
    }
    @Override
    public @NotNull PluginFactory getFactory() {
        return factory;
    }

    @Override
    public @Nullable String getName() {
        return name;
    }
    @Override
    public @NotNull PluginContext getContext() {
        return context;
    }

    @Override
    public @Nullable String getDescription() {
        return description;
    }

    @Override
    public @NotNull Class<?> getReference() {
        return reference;
    }

    @Override
    public @NotNull Handlers getHandlers() {
        return handlers;
    }

    @Override
    public @NotNull Class<?> @NotNull [] getDependencies() {
        return dependencies.toArray(new Class[0]);
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public @NotNull PluginInitializer getInitializer() {
        return initializer;
    }

    @Override
    public @NotNull Collection<String> getCategories() {
        return categories;
    }

    // Setters

    @Override
    public @NotNull Builder name(@Nullable String name) {
        this.name = name;
        return this;
    }
    @Override
    public @NotNull Builder description(@Nullable String description) {
        this.description = description;
        return this;
    }

    @Override
    public @NotNull Builder category(@NotNull String category) {
        this.categories.add(category);
        return this;
    }
    @Override
    public @NotNull Builder categories(@NotNull String @NotNull ... categories) {
        this.categories.clear();
        this.categories.addAll(Arrays.asList(categories));

        return this;
    }

    @Override
    public @NotNull Builder category(@NotNull PluginCategory category) {
        this.categories.add(category.getName());
        return this;
    }
    @Override
    public @NotNull Builder categories(@NotNull PluginCategory @NotNull ... categories) {
        this.categories.clear();
        this.categories.addAll(Arrays.stream(categories).map(PluginCategory::getName).collect(Collectors.toList()));

        return this;
    }

    @Override
    public @NotNull Builder dependency(@NotNull Class<?> dependency) {
        checkDependency(dependency);
        this.dependencies.add(dependency);

        return this;
    }
    @Override
    public @NotNull Builder dependency(@NotNull PluginInfo info) {
        checkDependency(info.getReference());

        this.dependencies.add(info.getReference());

        return this;
    }
    @Override
    public @NotNull Builder dependencies(@NotNull Class<?> @NotNull ... dependencies) {
        for (@NotNull Class<?> dependency : dependencies) {
            checkDependency(dependency);
        }

        this.dependencies.clear();
        this.dependencies.addAll(Arrays.asList(dependencies));

        return this;
    }
    @Override
    public @NotNull Builder dependencies(@NotNull PluginInfo @NotNull ... dependencies) {
        for (@NotNull Class<?> dependency : Arrays.stream(dependencies).map(PluginInfo::getReference).collect(Collectors.toList())) {
            checkDependency(dependency);
        }

        this.dependencies.clear();
        this.dependencies.addAll(Arrays.stream(dependencies).map(PluginInfo::getReference).collect(Collectors.toList()));

        return this;
    }

    @Override
    public @NotNull Builder priority(int priority) {
        this.priority = priority;
        return this;
    }

    @Override
    public @NotNull Builder initializer(@NotNull Class<? extends PluginInitializer> initializer) {
        this.initializer = getInitializerFactory().getInitializer(initializer);
        return this;
    }
    @Override
    public @NotNull Builder initializer(@NotNull PluginInitializer initializer) {
        this.initializer = initializer;
        return this;
    }

    // Builder

    @Override
    public @NotNull PluginInfo build() {
        return getInitializer().build(this);
    }

    // Implementations

    @Override
    public @NotNull String toString() {
        return getReference().getName();
    }

}
