package codes.laivy.plugin.initializer;

import codes.laivy.plugin.PluginInfo;
import codes.laivy.plugin.PluginInfo.Builder;
import codes.laivy.plugin.category.PluginCategory;
import codes.laivy.plugin.factory.handlers.Handlers;
import codes.laivy.plugin.main.Plugins;
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
    private @NotNull Class<? extends PluginInitializer> initializer = ConstructorPluginInitializer.class;

    private final @NotNull Set<PluginCategory> categories = new LinkedHashSet<>();
    private final @NotNull Set<PluginInfo> dependencies = new LinkedHashSet<>();

    private final @NotNull Handlers handlers = Handlers.create();

    public AbstractPluginBuilder(@NotNull Class<?> reference) {
        this.reference = reference;
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
    public @NotNull PluginInfo @NotNull [] getDependencies() {
        return dependencies.toArray(new PluginInfo[0]);
    }
    @Override
    public @NotNull PluginCategory @NotNull [] getCategories() {
        return categories.toArray(new PluginCategory[0]);
    }

    @Override
    public @NotNull Class<? extends PluginInitializer> getInitializer() {
        return initializer;
    }

    @Override
    public @NotNull Handlers getHandlers() {
        return handlers;
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
    public @NotNull Builder category(@NotNull PluginCategory category) {
        this.categories.add(category);
        return this;
    }
    @Override
    public @NotNull Builder categories(@NotNull PluginCategory @NotNull ... categories) {
        this.categories.clear();
        this.categories.addAll(Arrays.asList(categories));

        return this;
    }

    @Override
    public @NotNull Builder dependency(@NotNull Class<?> dependency) {
        this.dependencies.add(Plugins.retrieve(dependency));
        return this;
    }
    @Override
    public @NotNull Builder dependency(@NotNull PluginInfo info) {
        this.dependencies.add(info);
        return this;
    }
    @Override
    public @NotNull Builder dependencies(@NotNull Class<?> @NotNull ... dependencies) {
        this.dependencies.clear();
        this.dependencies.addAll(Arrays.stream(dependencies).map(Plugins::retrieve).collect(Collectors.toList()));

        return this;
    }
    @Override
    public @NotNull Builder dependencies(@NotNull PluginInfo @NotNull ... dependencies) {
        this.dependencies.clear();
        this.dependencies.addAll(Arrays.asList(dependencies));

        return this;
    }

    @Override
    public @NotNull Builder initializer(@NotNull Class<? extends PluginInitializer> initializer) {
        this.initializer = initializer;
        return this;
    }

}
