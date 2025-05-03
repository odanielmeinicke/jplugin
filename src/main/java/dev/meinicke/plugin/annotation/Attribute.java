package dev.meinicke.plugin.annotation;

import dev.meinicke.plugin.attribute.AttributeHolder;
import dev.meinicke.plugin.exception.IllegalAttributeTypeException;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.*;

/**
 * Declares a compile-time attribute to be associated with a plugin class.
 * <p>
 * This annotation is used to define metadata that can be read during plugin discovery or initialization,
 * and automatically injected into the plugin's {@link AttributeHolder}. Each attribute defines a key,
 * a type (to validate correctness), and one value (either a scalar or an array).
 * </p>
 *
 * <h3>Key Features:</h3>
 * <ul>
 *   <li>Supports primitive types, strings, class references, and their array counterparts.</li>
 *   <li>All keys are case-insensitive and must be unique per plugin class.</li>
 *   <li>Allows multiple attributes on the same class via {@link Attributes} (repeating container).</li>
 *   <li>Performs runtime type validation using the {@link #type()} element.</li>
 * </ul>
 *
 * <h3>Type Validation:</h3>
 * <p>
 * The {@link #type()} element defines the expected type that will be used by the
 * {@link AttributeHolder} for value injection. This type must match the actual kind of value
 * defined (e.g. {@code String.class}, {@code int[].class}, {@code MyEnum.class}).
 * If a mismatch is detected, an {@code IllegalAttributeTypeException} is thrown at runtime.
 * </p>
 *
 * <h3>Usage Constraints:</h3>
 * <ul>
 *   <li>Exactly one of the value elements must be non-default (e.g. {@code string()}, {@code integer()}, etc.).</li>
 *   <li>All unused elements must remain at their default values.</li>
 *   <li>The {@code type()} must correspond to a valid getter method in {@link AttributeHolder}.</li>
 *   <li>Array types are supported (e.g. {@code String[].class}, {@code int[].class}, etc.).</li>
 * </ul>
 *
 * <h3>Example:</h3>
 * <pre>{@code
 * \@Attribute(
 *     key = "max-connections",
 *     type = int.class,
 *     integer = 100
 * )
 * \@Attribute(
 *     key = "supported-modes",
 *     type = String[].class,
 *     stringArray = {"READ", "WRITE"}
 * )
 * public final class MyPlugin { ... }
 * }</pre>
 *
 * @see AttributeHolder
 * @see Attributes
 * @see IllegalAttributeTypeException
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Repeatable(Attributes.class)
public @interface Attribute {

    /**
     * The unique, case-insensitive key that identifies this attribute.
     * <p>
     * This key is used for lookup in {@link AttributeHolder} and must be unique per plugin.
     *
     * @return the key name; never {@code null} or empty
     */
    @NotNull String key();

    /**
     * Declares the expected Java type for this attribute’s value.
     * <p>
     * The framework will validate this type against the declared value (e.g., {@code string()}, {@code integer()})
     * and ensure it maps to a supported method in {@link AttributeHolder}.
     * </p>
     * <p>
     * If the provided value does not match the declared type, an {@link IllegalAttributeTypeException} will be thrown.
     * </p>
     *
     * @return the value type; must not be {@code void.class}
     */
    @NotNull Class<?> type();

    // --- String Types ---

    /**
     * A single {@code String} value for this attribute.
     * <p>
     * Default is the empty string. If this element is used, all other value elements (<em>except</em> {@code key})
     * must remain at their defaults.
     * </p>
     *
     * @return the string value, or empty if unused.
     */
    @NotNull String string() default "";

    /**
     * An array of {@code String} values for this attribute.
     * <p>
     * Default is an empty array. Exactly one of {@code stringValue} or {@code stringArray}
     * (or one of the other value elements) must be non-default.
     * </p>
     *
     * @return the string array, or empty if unused.
     */
    @NotNull String @NotNull [] stringArray() default {};

    // --- Class Types ---

    /**
     * A single {@code Class<?>} reference for this attribute.
     * <p>
     * Default is {@link Void}. Use this element only to supply a single class value.
     * </p>
     *
     * @return the class value, or {@link Void} if unused.
     */
    @NotNull Class<?> classValue() default Void.class;

    /**
     * An array of {@code Class<?>} references for this attribute.
     * <p>
     * Default is an empty array. Exactly one of {@code classValue} or {@code classArray}
     * (or one of the other value elements) must be non-default.
     * </p>
     *
     * @return the class array, or empty if unused.
     */
    @NotNull Class<?> @NotNull [] classArray() default {};

    // --- Primitive Single Values ---

    /**
     * An {@code int} value for this attribute.
     * <p>
     * Default is {@code 0}. If used, no other value element may be set.
     * </p>
     *
     * @return the int value, or {@code 0} if unused.
     */
    int integer() default 0;

    /**
     * A {@code long} value for this attribute.
     * <p>
     * Default is {@code 0L}. Only one single-value or array-value element may be non-default.
     * </p>
     *
     * @return the long value, or {@code 0L} if unused.
     */
    long longValue() default 0L;

    /**
     * A {@code double} value for this attribute.
     * <p>
     * Default is {@code 0.0}. Must be the only non-default value element.
     * </p>
     *
     * @return the double value, or {@code 0.0} if unused.
     */
    double doubleValue() default 0.0;

    /**
     * A {@code float} value for this attribute.
     * <p>
     * Default is {@code 0.0f}. Exactly one of the primitive or array value elements must be non-default.
     * </p>
     *
     * @return the float value, or {@code 0.0f} if unused.
     */
    float floatValue() default 0.0f;

    /**
     * A {@code boolean} flag for this attribute.
     * <p>
     * Default is {@code false}. If this is the chosen element, all others must remain default.
     * </p>
     *
     * @return the boolean value, or {@code false} if unused.
     */
    boolean booleanValue() default false;

    /**
     * A {@code byte} value for this attribute.
     *
     * @return the byte value, or {@code 0} if unused.
     */
    byte byteValue() default 0;

    /**
     * A {@code short} value for this attribute.
     *
     * @return the short value, or {@code 0} if unused.
     */
    short shortValue() default 0;

    /**
     * A {@code char} value for this attribute.
     *
     * @return the char value, or {@code '\0'} if unused.
     */
    char character() default '\0';

    // --- Primitive Array Values ---

    /**
     * An array of {@code int} values for this attribute.
     *
     * @return an int array, or empty if unused.
     */
    int[] integerArray() default {};

    /**
     * An array of {@code long} values for this attribute.
     *
     * @return a long array, or empty if unused.
     */
    long[] longArray() default {};

    /**
     * An array of {@code double} values for this attribute.
     *
     * @return a double array, or empty if unused.
     */
    double[] doubleArray() default {};

    /**
     * An array of {@code float} values for this attribute.
     *
     * @return a float array, or empty if unused.
     */
    float[] floatArray() default {};

    /**
     * An array of {@code boolean} values for this attribute.
     *
     * @return a boolean array, or empty if unused.
     */
    boolean[] booleanArray() default {};

    /**
     * An array of {@code byte} values for this attribute.
     *
     * @return a byte array, or empty if unused.
     */
    byte[] byteArray() default {};

    /**
     * An array of {@code short} values for this attribute.
     *
     * @return a short array, or empty if unused.
     */
    short[] shortArray() default {};

    /**
     * An array of {@code char} values for this attribute.
     *
     * @return a char array, or empty if unused.
     */
    char[] charArray() default {};

}