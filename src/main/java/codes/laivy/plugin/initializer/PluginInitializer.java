package codes.laivy.plugin.initializer;

import codes.laivy.plugin.exception.InvalidPluginException;
import codes.laivy.plugin.info.PluginInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Defines a strategy for initializing classes annotated with {@code @Plugin}.
 * Implementations of this interface are responsible for processing plugin metadata
 * and generating a corresponding {@link PluginInfo} instance that integrates seamlessly
 * with the plugin system.
 * <p>
 * Each implementing class <strong>must</strong> declare a no-argument constructor, which can
 * have any visibility modifier. This ensures that the initialization mechanism can
 * instantiate the class dynamically without requiring external parameters.
 * <p>
 * The primary purpose of this interface is to allow flexible and customizable plugin
 * initialization strategies while maintaining a standardized approach for retrieving
 * plugin-related metadata.
 * <p>
 * Implementations should handle validation, transformation, and dependency resolution
 * when constructing the {@link PluginInfo} instance. Additionally, they should ensure
 * that plugins conform to expected system constraints before being fully initialized.
 *
 * @author Daniel Meinicke
 * @since 1.0
 */
public interface PluginInitializer {
    /**
     * Constructs a {@link PluginInfo} instance using the provided class reference and associated metadata.
     * <p>
     * This method is responsible for extracting and processing plugin-related information,
     * including its name, description, dependencies, and category tags. It ensures that the
     * generated {@link PluginInfo} object accurately represents the plugin's characteristics.
     * <p>
     * If {@code name} or {@code description} are null, implementations should handle these cases
     * appropriately, either by assigning default values or leaving them unset, depending on
     * the intended behavior.
     * <p>
     * Implementations must also verify the validity of dependencies and category tags,
     * ensuring that they conform to any required constraints within the plugin system.
     *
     * @param reference    The class annotated with {@code @Plugin}. This serves as the primary
     *                     reference for identifying the plugin and extracting relevant metadata.
     * @param name         The display name of the plugin. This value may be null if the name
     *                     is not explicitly defined within the annotation or metadata.
     * @param description  A brief description of the plugin's purpose and functionality. This
     *                     value may be null if not explicitly specified.
     * @param dependencies An array of {@link PluginInfo} instances representing the dependencies
     *                     required by this plugin. This ensures proper dependency resolution
     *                     before the plugin is initialized.
     * @param categories   An array of category tags that classify the plugin within the system.
     *                     These tags help in organizing and filtering plugins based on their purpose.
     * @return A fully constructed {@link PluginInfo} instance encapsulating the provided metadata
     *         and initialization details.
     * @throws IllegalArgumentException If the provided class reference is invalid or does not conform
     *                                  to expected constraints.
     * @throws InvalidPluginException If an error occurs while processing dependencies or
     *                                       retrieving metadata.
     */
    @NotNull PluginInfo create(@NotNull Class<?> reference,
                               @Nullable String name,
                               @Nullable String description,
                               @NotNull PluginInfo @NotNull [] dependencies,
                               @NotNull String @NotNull [] categories) throws InvalidPluginException;
}