package dev.meinicke.plugin.annotation;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A container annotation that enables multiple {@link Category} annotations to be applied to a single plugin class.
 * <p>
 * This inner annotation is used by the Java compiler to group multiple instances of {@code @Category} when a plugin
 * is annotated with more than one category. It provides a single point of reference for all the categories assigned to
 * a plugin.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Categories {

    /**
     * An array of {@link Category} annotations.
     * <p>
     * This array contains all the {@code @Category} annotations applied to the plugin, allowing the framework to
     * process and associate all category-specific handlers and configurations at once.
     *
     * @return a non-null array of {@code @Category} annotations.
     */
    @NotNull Category @NotNull [] value();
}
