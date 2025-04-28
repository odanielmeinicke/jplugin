package dev.meinicke.plugin.annotation;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.*;

/**
 * Specifies a dependency required by a plugin.
 * <p>
 * The {@code @Dependency} annotation is used to declare a required dependency for a plugin class. It is repeatable
 * and can be used multiple times on the same class to specify multiple dependencies. The dependencies are declared
 * using the {@code type} element, which should reference a class that represents the required dependency.
 * <p>
 * The annotation is retained at runtime ({@link RetentionPolicy#RUNTIME}), allowing the framework to use reflection
 * to process and resolve dependencies dynamically.
 * <p>
 * This annotation is also {@code @Repeatable} with {@link Dependencies}, which allows multiple instances of
 * {@code @Dependency} to be used on the same type.
 * <p>
 * <strong>Example Usage:</strong>
 * <pre>{@code
 * Dependency(type = SomeLibrary.class)
 * Dependency(type = AnotherPlugin.class)
 * Plugin
 * public class MyPlugin {
 *     // Plugin implementation
 * }
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Repeatable(value = Dependency.Dependencies.class)
public @interface Dependency {
    /**
     * Specifies the class type that represents the required dependency.
     *
     * @return The dependency class type.
     */
    @NotNull Class<?> type();

    /**
     * A container annotation for multiple {@link Dependency} annotations.
     * <p>
     * This is used internally by Java's {@link Repeatable} mechanism to store multiple dependencies.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @interface Dependencies {
        /**
         * An array of {@link Dependency} annotations.
         *
         * @return The declared dependencies.
         */
        @NotNull Dependency @NotNull [] value();
    }
}
