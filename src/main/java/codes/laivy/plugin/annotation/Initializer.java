package codes.laivy.plugin.annotation;

import codes.laivy.plugin.initializer.PluginInitializer;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as an initializer for a plugin within the framework.
 * <p>
 * The {@code @Initializer} annotation is used to specify a {@link PluginInitializer} implementation responsible for
 * handling the setup and configuration of a plugin. This annotation is applied at the class level and allows the
 * framework to dynamically associate plugins with their respective initialization logic.
 * <p>
 * The annotation defines a single mandatory element:
 * <ul>
 *   <li>{@code type} - Specifies the {@link PluginInitializer} implementation that will be used to initialize
 *       the annotated plugin. This class must extend {@code PluginInitializer} and provide a concrete
 *       implementation of the initialization process.</li>
 * </ul>
 * <p>
 * The annotation is retained at runtime ({@link RetentionPolicy#RUNTIME}), allowing the plugin framework to use
 * reflection to detect and process it. It can only be applied to types (classes, interfaces, enums, or annotation
 * types), as indicated by the {@link Target} annotation.
 * <p>
 * <strong>Example Usage:</strong>
 * <pre>{@code
 * @Initializer(type = ConstructorPluginInitializer.class)
 * @Plugin(name = "Cool Plugin", description = "A cool plugin designed by Daniel Meinicke")
 * public class MyPlugin {
 *     // Plugin implementation details
 * }
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Initializer {
    /**
     * Specifies the {@link PluginInitializer} implementation responsible for initializing the annotated plugin.
     * <p>
     * The class provided must extend {@code PluginInitializer} and define the necessary setup logic for the plugin.
     *
     * @return The initializer class type.
     */
    @NotNull Class<? extends PluginInitializer> type();
}