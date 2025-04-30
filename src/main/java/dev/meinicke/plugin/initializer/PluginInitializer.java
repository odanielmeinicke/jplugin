package dev.meinicke.plugin.initializer;

import dev.meinicke.plugin.PluginInfo;
import dev.meinicke.plugin.context.PluginContext;
import dev.meinicke.plugin.exception.InvalidPluginException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Defines a strategy for initializing plugin classes annotated with {@code @Plugin}.
 * <p>
 * Implementations of this interface are responsible for analyzing the provided plugin class,
 * extracting its metadata, and creating a corresponding {@link PluginInfo} via a {@link PluginInfo.Builder},
 * which encapsulates all necessary configuration for correct integration with the plugin framework.
 * <p>
 * The {@link PluginInitializer} is the main extensibility point for controlling how a plugin is
 * interpreted and instantiated. Implementations may vary in how they introspect the plugin class,
 * validate constraints, resolve dependencies, and handle plugin categorization.
 *
 * <h3>Initialization Responsibilities</h3>
 * An implementation must:
 * <ul>
 *     <li>Interpret and validate plugin annotations and metadata.</li>
 *     <li>Resolve and enforce plugin dependencies and categories.</li>
 *     <li>Produce a {@link PluginInfo.Builder} that represents the plugin in the system.</li>
 *     <li>Ensure that when the resulting {@link PluginInfo} is built and later started, it adheres
 *         to the behavior and constraints defined by this initializer.</li>
 * </ul>
 *
 * <p>
 * As an example, the {@code MethodPluginInitializer} scans the plugin class for a method named
 * {@code initialize} (commonly with a {@link PluginContext} parameter). The {@code PluginInfo}
 * it produces, when invoked through {@link PluginInfo#start()}, dynamically locates and calls
 * this {@code initialize} method to bootstrap the plugin.
 *
 * <p>
 * The {@link PluginContext} parameter provides additional information and services to the initializer,
 * including metadata, runtime attributes, the caller class, and access to other loaded plugins.
 * This enables complex initialization logic based on the runtime state of the plugin system.
 *
 * @author Daniel Meinicke
 * @since 1.0
 */
public interface PluginInitializer {

    /**
     * Constructs a {@link PluginInfo.Builder} for a plugin class based on its metadata, dependencies,
     * and the current runtime plugin context.
     * <p>
     * This method acts as the primary entry point for transforming a plugin declaration into a structured
     * {@link PluginInfo.Builder}. It must extract all relevant metadata — such as name, description, categories,
     * and declared dependencies — and ensure consistency and validity.
     * <p>
     * If {@code name} or {@code description} are {@code null}, the initializer may infer them from annotations,
     * provide fallback defaults, or omit them, depending on the framework’s policy.
     *
     * <p><b>PluginContext Integration:</b></p>
     * The {@link PluginContext} gives access to the current state of the plugin system, including:
     * <ul>
     *     <li>The plugin currently being processed via {@link PluginContext#getCurrentPlugin()}.</li>
     *     <li>Other plugins already discovered or initialized via {@link PluginContext#getAllPlugins()}.</li>
     *     <li>Plugin-specific metadata and runtime-defined attributes for configuration purposes.</li>
     * </ul>
     * This enables context-sensitive initializers that can vary behavior based on environment or dependency state.
     *
     * @param reference    The plugin class annotated with {@code @Plugin}, serving as the main definition.
     * @param name         Optional name of the plugin, possibly derived from annotation or other metadata.
     * @param description  Optional description of the plugin’s purpose.
     * @param dependencies Array of classes representing required plugin dependencies.
     *                     These classes must themselves be recognized plugins.
     * @param categories   Array of string identifiers categorizing the plugin.
     * @param context      The {@link PluginContext} for the current initialization process,
     *                     providing access to framework state and metadata.
     * @return A configured {@link PluginInfo.Builder} instance, ready to be built and initialized.
     * @throws IllegalArgumentException If the plugin class is invalid or cannot be processed.
     * @throws InvalidPluginException   If validation or metadata resolution fails.
     */
    @NotNull PluginInfo.Builder create(@NotNull Class<?> reference,
                                       @Nullable String name,
                                       @Nullable String description,
                                       @NotNull Class<?> @NotNull [] dependencies,
                                       @NotNull String @NotNull [] categories,
                                       @NotNull PluginContext context) throws InvalidPluginException;
}