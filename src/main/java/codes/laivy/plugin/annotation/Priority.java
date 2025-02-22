package codes.laivy.plugin.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies the loading priority of a plugin, using an integer value where lower numbers indicate higher priority.
 * <p>
 * This annotation is applied to plugin classes to determine the order in which they should be initialized.
 * A plugin annotated with a lower numeric value is considered more important and will be loaded before plugins
 * with higher numeric values.
 * <p>
 * <strong>Default Behavior:</strong>
 * <ul>
 *   <li>
 *     If a plugin does not specify the {@code @Priority} annotation, the system will assume the default priority
 *     of {@code 0}, which represents a "normal" priority level.
 *   </li>
 *   <li>
 *     If a plugin <b>does</b> specify {@code @Priority} but does not explicitly define a value, the default priority
 *     will be {@code -1}. This means that any plugin annotated with {@code @Priority} without specifying a value
 *     will always have a higher priority than plugins that do not have this annotation.
 *   </li>
 *   <li>
 *     The framework will load plugins in ascending order of priority (i.e., a plugin with {@code @Priority(-5)}
 *     will load before a plugin with {@code @Priority(2)}).
 *   </li>
 * </ul>
 * <p>
 * <strong>How It Works:</strong>
 * <ul>
 *   <li>
 *     The priority value is expressed as an integer. For example, a plugin annotated with {@code @Priority(1)}
 *     will be loaded before a plugin annotated with {@code @Priority(10)}.
 *   </li>
 *   <li>
 *     In cases where two or more plugins have the same priority, their loading order may be determined by other
 *     criteria (such as dependency relationships or alphabetical order of the plugin names).
 *   </li>
 *   <li>
 *     Note that in a dependency-aware framework, dependency requirements always take precedence over the numeric
 *     priority. That is, if Plugin A (with any priority value) is a dependency of Plugin B, then Plugin A must be loaded
 *     before Plugin B, regardless of their specified priorities.
 *   </li>
 * </ul>
 * <p>
 * <strong>Usage Examples:</strong>
 * <pre>
 * {@code
 * // A plugin with high priority (will load early)
 * Plugin(name = "CorePlugin", description = "Handles core system functions")
 * Priority(1)
 * public class CorePlugin {
 *     // Plugin implementation details...
 * }
 *
 * // A plugin with lower priority (will load later)
 * Plugin(name = "ExtraPlugin", description = "Provides additional features")
 * Priority(10)
 * public class ExtraPlugin {
 *     // Plugin implementation details...
 * }
 *
 * // A plugin with default priority (-1), which will be loaded before plugins without @Priority
 * Plugin(name = "MiddlewarePlugin", description = "Handles middleware tasks")
 * Priority
 * public class MiddlewarePlugin {
 *     // Plugin implementation details...
 * }
 * }
 * </pre>
 * <p>
 * <strong>Customization:</strong>
 * <br>
 * By using a numeric priority, developers have a high degree of flexibility, allowing for fine-grained control
 * over the loading order. However, it is important to document and standardize the meaning of various priority levels
 * within your project to avoid confusion. For instance, you might decide that values -10 to -1 represent critical plugins,
 * values 0-5 represent standard plugins, and values greater than 5 represent optional or low-priority plugins.
 * <p>
 * This annotation is part of the plugin framework's strategy to manage plugin initialization order, ensuring that
 * essential components are available before others are loaded.
 *
 * @see Plugin
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Priority {
    /**
     * The integer value that defines the loading priority of the plugin.
     * <p>
     * Lower values indicate higher priority, meaning that a plugin with a value of -5 will be loaded
     * before a plugin with a value of 2. If no priority is explicitly specified, the default value is -1,
     * which represents a higher priority than plugins that do not have this annotation (priority 0).
     * <p>
     * <strong>Example:</strong>
     * <pre>
     * {@code
     * Priority(1)  // Higher priority: this plugin will be loaded early.
     * Plugin
     * public class ImportantPlugin { ... }
     *
     * Priority(10) // Lower priority: this plugin will be loaded after higher priority plugins.
     * Plugin
     * public class OptionalPlugin { ... }
     *
     * Priority // Equivalent to @Priority(-1), will load before plugins without @Priority.
     * Plugin
     * public class DefaultPriorityPlugin { ... }
     * }
     * </pre>
     *
     * @return an integer representing the plugin's priority; lower values indicate a higher priority.
     */
    int value() default -1;
}