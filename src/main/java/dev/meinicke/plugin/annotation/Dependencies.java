package dev.meinicke.plugin.annotation;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.*;

/**
 * A container annotation for multiple {@link Dependency} annotations.
 * <p>
 * This is used internally by Java's {@link Repeatable} mechanism to store multiple dependencies.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Dependencies {
    /**
     * An array of {@link Dependency} annotations.
     *
     * @return The declared dependencies.
     */
    @NotNull Dependency @NotNull [] value();
}
