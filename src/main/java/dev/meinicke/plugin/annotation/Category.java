package dev.meinicke.plugin.annotation;

import dev.meinicke.plugin.factory.handlers.PluginHandler;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.*;

/**
 * An annotation used to categorize a plugin within the framework.
 * <p>
 * The {@code @Category} annotation is intended to be used in conjunction with the {@link Plugin} annotation
 * to label a plugin with one or more specific categories. By declaring a category, developers can associate
 * a set of custom {@link PluginHandler} implementations with all plugins that share that category. This allows
 * for the centralized application of common behaviors or lifecycle management strategies, reducing the need for
 * repetitive code across multiple plugins.
 * <p>
 * Categories act as a means of grouping plugins by functionality, purpose, or any other classification that is meaningful
 * in the context of the application. For instance, a plugin designed for utility functions might be categorized as "Utility",
 * while another focused on security features might be labeled "Security". Handlers associated with these categories can then
 * be applied uniformly, enabling features such as logging, performance monitoring, or conditional initialization based on
 * the plugin’s category.
 * <p>
 * <strong>Case Insensitivity:</strong> The category name is not case-sensitive. This means that a category named "Utility"
 * is considered equivalent to a category named "utility", ensuring consistency in how plugins are grouped regardless of
 * letter case.
 * <p>
 * This annotation is repeatable, which means that a single plugin class can be associated with multiple categories.
 * The repeatability is enabled by the nested {@link Categories} annotation, which acts as a container for multiple
 * {@code @Category} annotations.
 * <p>
 * <strong>Usage Example:</strong>
 * <pre>{@code
 * Category(name  = "Utility")
 * Category(name  = "Performance")
 * Plugin(name  = "MyPlugin", description = "A plugin that provides utility and performance enhancements")
 * public class MyPlugin {
 *     // Plugin implementation details
 * }
 * }</pre>
 * <p>
 * <strong>Detailed Behavior:</strong>
 * <ul>
 *   <li><em>Identification:</em> The {@code name} element should be a unique identifier for the category.
 *       It is not case-sensitive; therefore, "Utility" and "utility" are treated as identical. This name is used
 *       to register and look up category-specific handlers and to differentiate between different groups of plugins.</li>
 *   <li><em>Handler Association:</em> By defining a category, any {@link PluginHandler} registered for that category
 *       will automatically be applied to the plugin. This facilitates modular and reusable event handling, such as logging,
 *       metrics, or error handling specific to that category.</li>
 *   <li><em>Repeatability:</em> Since the annotation is marked as {@code @Repeatable}, developers can annotate a plugin
 *       with multiple {@code @Category} annotations. This allows a plugin to participate in multiple groupings and have handlers
 *       applied from several categories simultaneously.</li>
 *   <li><em>Retention and Target:</em> The annotation is retained at runtime (via {@link RetentionPolicy#RUNTIME}),
 *       ensuring that it is available for reflection during plugin discovery and initialization. It is applicable to types
 *       (classes, interfaces, enums, or annotation types) as specified by the {@link Target} annotation.</li>
 * </ul>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Repeatable(value = Categories.class)
// todo: @Global annotation that allows plugins to be loaded by another class loaders and/or packages
public @interface Category {

    /**
     * The name of the category.
     * <p>
     * This value serves as a unique identifier for the category and is used to associate plugins with specific
     * sets of handlers. It is not case-sensitive, meaning that "Utility" and "utility" are considered equivalent.
     *
     * @return a non-null string representing the category name.
     */
    @NotNull String value();
}