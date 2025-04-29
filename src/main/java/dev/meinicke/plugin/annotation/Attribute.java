package dev.meinicke.plugin.annotation;

import dev.meinicke.plugin.exception.IllegalAttributeTypeException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.*;

import static dev.meinicke.plugin.annotation.Attribute.Attributes;

/**
 * Declares a single metadata entry for a plugin, as defined by the {@code @Attribute} annotation.
 * <p>
 * Each usage of {@code @Attribute} must supply exactly one non-default value element (stringValue, stringArray,
 * classValue, classArray, intValue, intArray, etc.). Supplying more than one or none will result in undefined
 * behavior at runtime when processed by the plugin framework.
 * </p>
 *
 * <h3>Key Characteristics:</h3>
 * <ul>
 *   <li><strong>Key:</strong> A mandatory, case-insensitive identifier for this attribute.</li>
 *   <li><strong>Single Value Constraint:</strong> Exactly one of the value elements must be explicitly set to a
 *       non-default value. All other value elements must remain at their default.</li>
 *   <li><strong>Type Safety:</strong> At runtime, the framework will convert the declared value to the corresponding
 *       Java type and throw an {@link IllegalAttributeTypeException} if a mismatch occurs.</li>
 *   <li><strong>Repeatable:</strong> Multiple {@code @Attribute} annotations may be applied to the same element
 *       if the annotation type is marked {@code @Repeatable}; the framework will aggregate them.</li>
 * </ul>
 *
 * @see IllegalAttributeTypeException
 * @author Daniel Meinicke
 * @since 1.1.6
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Repeatable(Attributes.class)
public @interface Attribute {

    /**
     * The unique, case-insensitive key for this attribute entry.
     * Plugins and the plugin runtime use this key to look up and retrieve the associated value.
     *
     * @return the attribute key, never {@code null} or empty.
     */
    @NotNull String key();

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
    @NotNull String stringValue() default "";

    /**
     * An array of {@code String} values for this attribute.
     * <p>
     * Default is an empty array. Exactly one of {@code stringValue} or {@code stringArray}
     * (or one of the other value elements) must be non-default.
     * </p>
     *
     * @return the string array, or empty if unused.
     */
    @NotNull String @Nullable [] stringArray() default {};

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
    @NotNull Class<?> @Nullable [] classArray() default {};

    // --- Primitive Single Values ---

    /**
     * An {@code int} value for this attribute.
     * <p>
     * Default is {@code 0}. If used, no other value element may be set.
     * </p>
     *
     * @return the int value, or {@code 0} if unused.
     */
    int intValue() default 0;

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
    char charValue() default '\0';

    // --- Primitive Array Values ---

    /**
     * An array of {@code int} values for this attribute.
     *
     * @return an int array, or empty if unused.
     */
    int[] intArray() default {};

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

    // Classes

    /**
     * Container annotation for repeating {@link Attribute} annotations.
     * <p>
     * This annotation enables multiple {@code @Attribute} declarations to be applied to the same element,
     * in accordance with the {@link java.lang.annotation.Repeatable} contract.
     * <p>
     * This container is automatically generated by the compiler when multiple {@link Attribute} annotations
     * are used on a single element, and is not typically used directly.
     * </p>
     *
     * @see Attribute
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @interface Attributes {

        /**
         * The array of {@link Attribute} annotations.
         *
         * @return the repeated attribute declarations
         */
        Attribute[] value();
    }

}