package dev.meinicke.plugin.annotation;

import dev.meinicke.plugin.initializer.PluginInitializer;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the annotated class is a plugin component managed by the framework.
 * <p>
 * The {@code @Plugin} annotation is used to mark a class as a plugin, enabling the framework to discover,
 * initialize, and manage it automatically. When a class is annotated with {@code @Plugin}, it is expected
 * to conform to the requirements of the plugin system, such as providing a suitable initialization strategy
 * (for example, via a designated {@link PluginInitializer}).
 * <p>
 * The annotation defines two optional elements:
 * <ul>
 *   <li>{@code name} - A human-readable name for the plugin. If left empty, the framework may use the class name
 *       as a fallback identifier. This name can be used for display purposes, logging, or filtering plugins.</li>
 *   <li>{@code description} - A brief description of the plugin's functionality. This element provides additional
 *       context about what the plugin does, and may be used in documentation or user interfaces to help users understand
 *       the plugin's purpose.</li>
 * </ul>
 * <p>
 * Both elements default to an empty string if not explicitly specified, meaning that the plugin will not have a name or
 * description unless provided by the developer.
 * <p>
 * The annotation is retained at runtime ({@link RetentionPolicy#RUNTIME}), which allows the plugin framework to use
 * reflection to detect and process annotated classes. It can only be applied to types (classes, interfaces, enums, or
 * annotation types), as indicated by the {@link Target} annotation.
 * <p>
 * <strong>Example Usage:</strong>
 * <pre>{@code
 * Plugin(name = "MyPlugin", description = "A sample plugin for demonstration purposes")
 * public class MyPlugin {
 *     // Plugin implementation details
 * }
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Plugin {
    /**
     * The name of the plugin.
     * <p>
     * This element provides a human-readable identifier for the plugin. It is used for display purposes, logging,
     * and as a key in plugin retrieval operations. If not explicitly specified, it defaults to an empty string, and
     * the framework may use the plugin class's name as an alternative identifier.
     *
     * @return The plugin name.
     */
    @NotNull String name() default "";

    /**
     * A brief description of the plugin's functionality.
     * <p>
     * This element offers a short summary of what the plugin does, which can be useful for documentation,
     * configuration interfaces, and debugging purposes. If not specified, it defaults to an empty string.
     *
     * @return The plugin description.
     */
    @NotNull String description() default "";
}