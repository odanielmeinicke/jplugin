package codes.laivy.plugin.initializer;

import codes.laivy.plugin.PluginInfo;
import codes.laivy.plugin.PluginInfo.Builder;
import codes.laivy.plugin.annotation.Priority;
import codes.laivy.plugin.category.PluginCategory;
import codes.laivy.plugin.factory.handlers.Handlers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

abstract class AbstractPluginBuilder implements Builder {

    // Object

    private final @NotNull Class<?> reference;

    private @Nullable String name;
    private @Nullable String description;
    protected @NotNull Class<? extends PluginInitializer> initializer = ConstructorPluginInitializer.class;

    protected final @NotNull Set<String> unregisteredCategories = new LinkedHashSet<>();
    protected final @NotNull Set<PluginCategory> registeredCategories = new LinkedHashSet<>();

    protected final @NotNull Set<Class<?>> dependencies = new LinkedHashSet<>();
    private int priority = 0;

    private final @NotNull Handlers handlers = Handlers.create();

    public AbstractPluginBuilder(@NotNull Class<?> reference) {
        this.reference = reference;

        if (reference.isAnnotationPresent(Priority.class)) {
            this.priority = reference.getAnnotation(Priority.class).value();
        }
    }

    // Getters

    @Override
    public @Nullable String getName() {
        return name;
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
        this.unregisteredCategories.add(category);
        return this;
    }
    @Override
    public @NotNull Builder categories(@NotNull String @NotNull ... categories) {
        this.unregisteredCategories.clear();
        this.unregisteredCategories.addAll(Arrays.asList(categories));

        return this;
    }

    @Override
    public @NotNull Builder category(@NotNull PluginCategory category) {
        this.registeredCategories.add(category);
        return this;
    }
    @Override
    public @NotNull Builder categories(@NotNull PluginCategory @NotNull ... categories) {
        this.registeredCategories.clear();
        this.registeredCategories.addAll(Arrays.asList(categories));

        return this;
    }

    @Override
    public @NotNull Builder dependency(@NotNull Class<?> dependency) {
        this.dependencies.add(dependency);
        return this;
    }
    @Override
    public @NotNull Builder dependency(@NotNull PluginInfo info) {
        this.dependencies.add(info.getReference());
        return this;
    }
    @Override
    public @NotNull Builder dependencies(@NotNull Class<?> @NotNull ... dependencies) {
        this.dependencies.clear();
        this.dependencies.addAll(Arrays.asList(dependencies));

        return this;
    }
    @Override
    public @NotNull Builder dependencies(@NotNull PluginInfo @NotNull ... dependencies) {
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
        this.initializer = initializer;
        return this;
    }

    // Classes

    private final class DefaultComparable implements Comparable<Builder> {
        @Override
        public int compareTo(@NotNull Builder o) {
            int a = getReference().isAnnotationPresent(Priority.class) ? getReference().getAnnotation(Priority.class).value() : 0;
            int b = o.getReference().isAnnotationPresent(Priority.class) ? o.getReference().getAnnotation(Priority.class).value() : 0;

            return Integer.compare(a, b);
        }
    }

}
