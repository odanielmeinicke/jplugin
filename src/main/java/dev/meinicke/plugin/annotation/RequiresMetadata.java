package dev.meinicke.plugin.annotation;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Container annotation to allow multiple {@link RequireMetadata} on the same type.
 *
 * @see RequireMetadata
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RequiresMetadata {

    /**
     * The array of {@link RequireMetadata} annotations to apply.
     *
     * @return the array of metadata requirements
     */
    @NotNull RequireMetadata @NotNull [] value();
}
