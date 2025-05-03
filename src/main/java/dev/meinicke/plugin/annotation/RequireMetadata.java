package dev.meinicke.plugin.annotation;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.*;

/**
 * Indicates that a plugin requires the presence of specific compile‑time or runtime metadata
 * entries to be initialized correctly. When a plugin class is annotated with
 * {@code @RequireMetadata}, the framework will verify that for each declared key/type pair,
 * a corresponding entry exists in the {@link dev.meinicke.plugin.context.PluginContext#getMetadata()}
 * and—if a non-void type is specified—that the metadata value is assignable to that type.
 *
 * <p><b>Modes of Requirement:</b></p>
 * <ul>
 *   <li><b>Typed Requirement:</b> If {@link #type()} is not {@code void.class}, the framework ensures
 *       both presence of the key and that the value is of the specified type (or convertible). Failing
 *       either check throws a {@link dev.meinicke.plugin.exception.PluginInitializeException}.</li>
 *   <li><b>Presence‑Only Requirement:</b> If {@link #type()} is left as its default {@code void.class},
 *       only the presence of the metadata key is enforced; no type validation is performed.</li>
 * </ul>
 *
 * <h3>Behavior</h3>
 * <ol>
 *   <li>Before building the {@link dev.meinicke.plugin.PluginInfo}, the framework checks
 *       {@code context.getMetadata().containsKey(key)}.</li>
 *   <li>If {@link #type()} ≠ {@code void.class}, it also verifies that the runtime value
 *       is assignable to the specified type; conversion failures or mismatches cause
 *       {@link dev.meinicke.plugin.exception.PluginInitializeException}.</li>
 *   <li>Missing keys (in either mode) halt initialization with a clear exception.</li>
 *   <li>Repeatable via the {@link RequiresMetadata} container for multiple requirements.</li>
 * </ol>
 *
 * @author Daniel Meinicke
 * @since 1.1.7
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Repeatable(RequiresMetadata.class)
public @interface RequireMetadata {

    /**
     * The case‑insensitive metadata key that must be present in the plugin context.
     *
     * @return the required metadata key; never {@code null} or empty
     */
    @NotNull String key();

    /**
     * The expected Java type of the metadata value associated with {@link #key()}.
     * <ul>
     *     <li>If set to a class other than {@code void.class} or {@code Void.class}, the framework enforces that the metadata value is assignable to this type.</li>
     *     <li>If left as the default {@code void.class} or {@code Void.class}, only the presence of the key is enforced, with no type validation.</li>
     * </ul>
     *
     * @return the required metadata value type, never {@code null}
     */
    @NotNull Class<?> type() default void.class;

}