package codes.laivy.plugin.annotation;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.*;

/**
 * An annotation that represents a category for a plugin. This annotation
 * is to be used in conjunction with the {@link Plugin} annotation.
 * By specifying a category, developers can add special handlers to all
 * plugins that share that specific category. This helps to streamline
 * the usage and avoid repetitive code across different plugins.
 *
 * <p>Example usage:</p>
 * <pre>
 * {@code
 * @Category(name = "Utility")
 * @Plugin
 * public class MyPlugin {
 *     // Plugin implementation
 * }
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Repeatable(value = Category.Categories.class)
public @interface Category {

    /**
     * The name of the category. This should be a unique identifier for
     * the category that the plugin belongs to.
     *
     * @return the name of the category
     */
    @NotNull String name();

    /**
     * An inner annotation that allows multiple {@link Category} annotations
     * to be defined on a single plugin class. This is used to group
     * multiple categories together.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @interface Categories {

        /**
         * An array of {@link Category} annotations. This allows the
         * specification of multiple categories for a single plugin.
         *
         * @return an array of categories
         */
        @NotNull Category @NotNull [] value();
    }
}