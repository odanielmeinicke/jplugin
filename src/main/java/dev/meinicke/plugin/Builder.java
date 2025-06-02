package dev.meinicke.plugin;

import dev.meinicke.plugin.category.PluginCategory;
import dev.meinicke.plugin.context.PluginContext;
import dev.meinicke.plugin.factory.InitializerFactory;
import dev.meinicke.plugin.factory.PluginFactory;
import dev.meinicke.plugin.factory.handlers.Handlers;
import dev.meinicke.plugin.factory.handlers.PluginHandler;
import dev.meinicke.plugin.initializer.PluginInitializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * A fluent builder interface for constructing a {@link PluginInfo} instance.
 * <p>
 * This interface provides methods to configure all the necessary properties of a plugin,
 * such as its name, description, class reference, dependencies, initializer, and associated handlers.
 * The builder follows a fluent API design, allowing for method chaining and incremental configuration.
 * <p>
 * Usage Example:
 * <pre>{@code
 * PluginInfo info = PluginInfo.builder()
 *     .name("Example Plugin")
 *     .description("A plugin for demonstration purposes")
 *     .reference(ExamplePlugin.class)
 *     .dependencies(DependencyA.class, DependencyB.class)
 *     .initializer(ExamplePluginInitializer.class)
 *     .category("Economy")
 *     .categories("Utility", "Fun")
 *     .build();
 * }</pre>
 * <p>
 * The builder exposes getter methods to retrieve the current configuration values and
 * setter methods to modify these values.
 */
public interface Builder {

    // Getters

    /**
     * Returns the current factory from this builder
     *
     * @return the factory of this builder
     */
    @NotNull PluginFactory getFactory();

    /**
     * Returns the current initializer factory from this builder
     *
     * @return the initializer factory of this builder
     */
    @NotNull InitializerFactory getInitializerFactory();

    /**
     * Returns the configured name of the plugin.
     *
     * @return A non-null string representing the plugin's name.
     */
    @Nullable String getName();

    /**
     * Returns the configured description of the plugin.
     *
     * @return A non-null string representing the plugin's description.
     */
    @Nullable String getDescription();

    /**
     * Returns the class reference of the plugin being built.
     *
     * @return The non-null Class object representing the plugin.
     */
    @NotNull Class<?> getReference();

    /**
     * Returns the dependencies of this builder.
     *
     * @return The non-null class array object representing the dependencies.
     */
    @NotNull Class<?> @NotNull [] getDependencies();

    /**
     * Returns the collection of {@link PluginHandler} instances associated with the plugin.
     *
     * @return A non-null Handlers instance.
     */
    @NotNull Handlers getHandlers();

    /**
     * Returns the context of the plugin.
     *
     * @return A non-null context of the plugin.
     */
    @NotNull PluginContext getContext();

    /**
     * Retrieves the current numeric priority value configured in this Builder.
     * <p>
     * The priority value is used to determine the order in which plugins are loaded by the framework.
     * Lower numeric values indicate higher priority, meaning that a plugin with a priority of 1 will be loaded
     * before a plugin with a priority of 10. This value is a key criterion during the plugin initialization process,
     * especially when multiple plugins are competing for early loading.
     * <p>
     * If no explicit priority is set using the {@link #priority(int)} method, the default value is 0. You can
     * customize it using the {@code @Priority} annotation (or an explicitly set priority).
     *
     * @return an integer representing the current priority value of this Builder; lower numbers denote higher priority.
     */
    int getPriority();

    @NotNull Collection<String> getCategories();
    @NotNull PluginInitializer getInitializer();

    // Setters

    /**
     * Sets the name for the plugin.
     *
     * @param name A non-null string representing the plugin's name.
     * @return This Builder instance for chaining.
     */
    @NotNull Builder name(@Nullable String name);

    /**
     * Sets the numeric priority for this Builder.
     * <p>
     * The priority value determines the order in which the plugin is loaded relative to other plugins.
     * Lower values indicate a higher priority. For instance, setting a priority of 1 will cause this plugin to be loaded
     * before a plugin with a priority of 10.
     * <p>
     * If no explicit priority is set using this method, the default value is 0. You can
     * customize it using the {@code @Priority} annotation (or an explicitly set priority with this method).
     * <p>
     * This priority setting works in tandem with dependency ordering; even if a plugin has a high priority (lower number),
     * it will still be loaded only after all of its dependencies have been resolved and loaded.
     *
     * @param priority the integer value to assign as the plugin's priority; lower values denote higher priority.
     * @return the current {@link Builder} instance, allowing for method chaining.
     */
    @NotNull Builder priority(int priority);

    /**
     * Sets the description for the plugin.
     *
     * @param description A non-null string representing the plugin's description.
     * @return This Builder instance for chaining.
     */
    @NotNull Builder description(@Nullable String description);

    /**
     * Adds a single category by the name to the plugin's list of categories.
     *
     * @param category A non-null category name.
     * @return This Builder instance for chaining.
     */
    @NotNull Builder category(@NotNull String category);

    /**
     * Adds multiple categories names to the plugin's list of categories.
     *
     * @param categories An array of non-null categories names.
     * @return This Builder instance for chaining.
     */
    @NotNull Builder categories(@NotNull String @NotNull ... categories);

    /**
     * Adds a single category to the plugin's list of categories.
     *
     * @param category A non-null category.
     * @return This Builder instance for chaining.
     */
    @NotNull Builder category(@NotNull PluginCategory category);

    /**
     * Adds multiple categories to the plugin's list of categories.
     *
     * @param categories An array of non-null categories.
     * @return This Builder instance for chaining.
     */
    @NotNull Builder categories(@NotNull PluginCategory @NotNull ... categories);

    /**
     * Adds a single dependency to the plugin's list of dependencies.
     *
     * @param dependency A non-null dependency.
     * @return This Builder instance for chaining.
     */
    @NotNull Builder dependency(@NotNull Class<?> dependency);

    /**
     * Adds a single dependency to the plugin's list of dependencies.
     *
     * @param dependency A non-null dependency.
     * @return This Builder instance for chaining.
     */
    @NotNull Builder dependency(@NotNull PluginInfo dependency);

    /**
     * Sets the dependencies of the plugin.
     *
     * @param dependencies An array of non-null Class objects that the plugin depends on.
     * @return This Builder instance for chaining.
     */
    @NotNull Builder dependencies(@NotNull Class<?> @NotNull ... dependencies);

    /**
     * Sets the dependencies of the plugin.
     *
     * @param dependencies An array of non-null plugin info objects that the plugin depends on.
     * @return This Builder instance for chaining.
     */
    @NotNull Builder dependencies(@NotNull PluginInfo @NotNull ... dependencies);

    /**
     * Sets the {@link PluginInitializer} to be used for initializing the plugin.
     *
     * @param initializer A non-null Class object extending PluginInitializer.
     * @return This Builder instance for chaining.
     */
    @NotNull Builder initializer(@NotNull Class<? extends PluginInitializer> initializer);

    /**
     * Sets the {@link PluginInitializer} to be used for initializing the plugin.
     *
     * @param initializer A non-null object of the PluginInitializer.
     * @return This Builder instance for chaining.
     */
    @NotNull Builder initializer(@NotNull PluginInitializer initializer);

    /**
     * Builds and returns a fully configured {@link PluginInfo} instance.
     * <p>
     * This method finalizes the builder configuration and constructs a {@link PluginInfo}
     * object with all the specified parameters. Once built, further modifications to the
     * builder instance will not affect the created {@link PluginInfo} instance.
     *
     * @return A new {@link PluginInfo} instance containing the configured properties.
     */
    @NotNull PluginInfo build();

}
