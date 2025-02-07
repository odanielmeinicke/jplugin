package codes.laivy.plugin.category;

import codes.laivy.plugin.PluginInfo;
import codes.laivy.plugin.factory.handlers.Handlers;
import codes.laivy.plugin.factory.handlers.PluginHandler;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * Represents a plugin category within the system, providing an immutable way to associate
 * a set of handlers and specific rules with a group of plugins. This interface extends
 * {@link PluginHandler} so that the category itself can act as a fixed and immutable handler,
 * preventing its configuration from being dynamically altered after definition.
 * <p>
 * Categories allow grouping plugins with similar functionalities or characteristics so that
 * specific handlers can be applied centrally to all plugins within that category. This approach
 * facilitates the application of policies, validations, configurations, and custom actions,
 * improving system modularity and maintainability.
 * <p>
 * <strong>Main functionalities:</strong>
 * <ul>
 *   <li>
 *     {@link #getName()} - Returns the unique name of the category, which is case-insensitive,
 *     ensuring that "Utility" and "utility" are considered equivalent.
 *   </li>
 *   <li>
 *     {@link #getHandlers()} - Provides the immutable set of handlers associated with the category,
 *     allowing consistent application of specific behaviors without the risk of future modifications.
 *   </li>
 *   <li>
 *     {@link #getPlugins()} - Returns a collection of {@link PluginInfo} instances representing the
 *     plugins associated with the category. This list is directly linked to the plugin's category list
 *     (obtained via {@link PluginInfo#getCategories()}); removing a plugin from this collection will
 *     also remove it from the plugin's category list.
 *   </li>
 * </ul>
 * <p>
 * <strong>Benefits and use cases:</strong>
 * <ul>
 *   <li>
 *     <em>Centralized management:</em> By associating immutable handlers with a category, all actions
 *     and behaviors defined for that category are uniformly applied to all plugins within it.
 *   </li>
 *   <li>
 *     <em>Data integrity:</em> The link between the plugin collection returned by {@link #getPlugins()}
 *     and the categories declared in plugins ensures consistency, so any modification in the association
 *     is reflected on both sides.
 *   </li>
 * </ul>
 * <p>
 * This interface is fundamental to plugin configuration and management, as it enables defining and applying
 * specific policies robustly and immutably, ensuring that the logic associated with a given category
 * is not inadvertently altered during system execution.
 *
 * @see PluginHandler
 * @see PluginInfo#getCategories()
 * @see Handlers
 */
public interface PluginCategory extends PluginHandler {

    /**
     * Returns the name of the category.
     * <p>
     * This name serves as the unique identifier for the category within the system and is case-insensitive,
     * meaning "Utility" and "utility" are treated as equivalent. The name is used to associate specific handlers
     * and to categorize plugins.
     *
     * @return a non-null string representing the category name.
     */
    @NotNull String getName();

    /**
     * Returns the set of handlers associated with this category.
     * <p>
     * This {@link Handlers} instance is immutable and represents the actions or behaviors
     * that will be applied to all plugins belonging to this category, ensuring consistent rule enforcement.
     *
     * @return a non-null {@link Handlers} instance for the category.
     */
    @NotNull Handlers getHandlers();

    /**
     * Returns the collection of plugins that belong to this category.
     * <p>
     * The returned collection directly affects the plugin's category association. If a plugin is removed from this
     * list, it will also be removed from the category list of the plugin itself, as defined in
     * {@link PluginInfo#getCategories()}, maintaining relationship integrity and consistency.
     *
     * @return a non-null collection of {@link PluginInfo} instances representing the plugins associated with this category.
     */
    @NotNull Collection<@NotNull PluginInfo> getPlugins();

}