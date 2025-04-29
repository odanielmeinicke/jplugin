package dev.meinicke.plugin.attribute;

import dev.meinicke.plugin.exception.IllegalAttributeTypeException;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a container for a plugin attribute defined via the {@code @Attribute} annotation.
 * <p>
 * This interface provides methods for accessing and converting the attribute value into specific types.
 * It is designed to be used by plugin initialization systems, execution environments, and any reflective mechanisms
 * that rely on custom plugin metadata.
 * <p>
 * The {@code key} is case-insensitive, and the {@code value} is strongly typed at runtime.
 * Convenience methods are provided for retrieving values with type-safety checks, throwing
 * {@link IllegalAttributeTypeException} when type mismatches occur.
 *
 * @author Daniel Meinicke
 * @since 1.1.6
 */
public interface AttributeHolder {

    /**
     * Returns the key of the attribute, as defined in the {@code @Attribute} annotation.
     * The key is treated in a case-insensitive manner during lookups and comparisons.
     *
     * @return the case-insensitive key associated with this attribute.
     */
    @NotNull String getKey();
    /**
     * Returns the raw value of the attribute, which may be of any type.
     * The exact type depends on how the attribute was defined in the annotation.
     *
     * @return the raw attribute value.
     */
    @NotNull Object getValue();

    /**
     * Returns the value of the attribute as a String, if its type is {@link String}.
     *
     * @return the string value of this attribute.
     * @throws IllegalAttributeTypeException if the value is not of type {@code String}.
     */
    default @NotNull String getAsString() throws IllegalAttributeTypeException {
        if (getValue() instanceof String) {
            return (String) getValue();
        } else {
            throw new IllegalAttributeTypeException("the attribute \"" + getKey() + "\" uses a value \"" + getValue().getClass().getName() + "\" and cannot be converted to string.");
        }
    }
    /**
     * Checks whether the value is of type {@code String}.
     *
     * @return true if the value is a String, false otherwise.
     */
    default boolean isString() {
        return getValue() instanceof String;
    }
    /**
     * Returns the value as a {@code String[]} if compatible.
     *
     * @return a string array representation of the value.
     * @throws IllegalAttributeTypeException if the value is not a {@code String[]}.
     */
    default @NotNull String[] getAsStringArray() throws IllegalAttributeTypeException {
        if (getValue() instanceof String[]) {
            return (String[]) getValue();
        } else {
            throw new IllegalAttributeTypeException("the attribute \"" + getKey() + "\" uses a value \"" + getValue().getClass().getName() + "\" and cannot be converted to string array.");
        }
    }
    /**
     * Checks whether the value is of type {@code String[]}.
     *
     * @return true if the value is a string array.
     */
    default boolean isStringArray() {
        return getValue() instanceof String[];
    }

    /**
     * Returns the value as a {@code Class<?>}, if it is a {@link Class}.
     *
     * @return the class value of this attribute.
     * @throws IllegalAttributeTypeException if the value is not of type {@code Class}.
     */
    default @NotNull Class<?> getAsClass() throws IllegalAttributeTypeException {
        if (getValue() instanceof Class) {
            return (Class<?>) getValue();
        } else {
            throw new IllegalAttributeTypeException("the attribute \"" + getKey() + "\" uses a value \"" + getValue().getClass().getName() + "\" and cannot be converted to class.");
        }
    }
    /**
     * Checks whether the value is a {@link Class}.
     *
     * @return true if the value is a class reference.
     */
    default boolean isClass() {
        return getValue() instanceof Class;
    }
    /**
     * Returns the value as a {@code Class<?>[]} if it is compatible.
     *
     * @return a class array representation of the value.
     * @throws IllegalAttributeTypeException if the value is not a {@code Class<?>[]}.
     */
    default @NotNull Class<?>[] getAsClassArray() throws IllegalAttributeTypeException {
        if (getValue() instanceof Class<?>[]) {
            return (Class<?>[]) getValue();
        } else {
            throw new IllegalAttributeTypeException("the attribute \"" + getKey() + "\" uses a value \"" + getValue().getClass().getName() + "\" and cannot be converted to class array.");
        }
    }
    /**
     * Checks whether the value is a {@code Class[]} array.
     *
     * @return true if the value is a class array.
     */
    default boolean isClassArray() {
        return getValue() instanceof Class[];
    }

    // --- Integer Accessors --- //

    /**
     * Returns the value as an {@code int}, if the underlying value is an {@link Integer}.
     *
     * @return the integer value.
     * @throws IllegalAttributeTypeException if the value is not an Integer.
     */
    default int getAsInteger() throws IllegalAttributeTypeException {
        if (getValue() instanceof Integer) {
            return (int) getValue();
        } else {
            throw new IllegalAttributeTypeException("the attribute \"" + getKey() + "\" uses a value \"" + getValue().getClass().getName() + "\" and cannot be converted to integer.");
        }
    }
    /**
     * Checks if the value is an {@code Integer}.
     *
     * @return true if the value is an integer.
     */
    default boolean isInteger() {
        return getValue() instanceof Integer;
    }
    /**
     * Returns the value as a primitive {@code int[]} or converts from {@code Integer[]}.
     *
     * @return the int array representation of the value.
     * @throws IllegalAttributeTypeException if the value cannot be converted.
     */
    default int[] getAsIntegerArray() throws IllegalAttributeTypeException {
        if (getValue() instanceof Integer[]) {
            @NotNull Integer[] value = (Integer[]) getValue();
            int[] array = new int[value.length];

            for (int index = 0; index < value.length; index++) {
                array[index] = value[index];
            }

            return array;
        } else if (getValue() instanceof int[]) {
            return (int[]) getValue();
        } else {
            throw new IllegalAttributeTypeException("the attribute \"" + getKey() + "\" uses a value \"" + getValue().getClass().getName() + "\" and cannot be converted to integer array.");
        }
    }
    /**
     * Checks whether the value is an {@code int[]} or {@code Integer[]}.
     *
     * @return true if the value is an integer array.
     */
    default boolean isIntegerArray() {
        return getValue() instanceof Integer[] || getValue() instanceof int[];
    }

    // --- Long, Double, Float, Boolean, Byte, Short, Character --- //
    // Each of the following follows the same documentation pattern from int accesors.

    default long getAsLong() throws IllegalAttributeTypeException {
        if (getValue() instanceof Long) {
            return (long) getValue();
        } else {
            throw new IllegalAttributeTypeException("the attribute \"" + getKey() + "\" uses a value \"" + getValue().getClass().getName() + "\" and cannot be converted to long.");
        }
    }
    default boolean isLong() {
        return getValue() instanceof Long;
    }
    default long[] getAsLongArray() throws IllegalAttributeTypeException {
        if (getValue() instanceof Long[]) {
            @NotNull Long[] value = (Long[]) getValue();
            long[] array = new long[value.length];

            for (int index = 0; index < value.length; index++) {
                array[index] = value[index];
            }

            return array;
        } else if (getValue() instanceof long[]) {
            return (long[]) getValue();
        } else {
            throw new IllegalAttributeTypeException("the attribute \"" + getKey() + "\" uses a value \"" + getValue().getClass().getName() + "\" and cannot be converted to long array.");
        }
    }
    default boolean isLongArray() {
        return getValue() instanceof Long[] || getValue() instanceof long[];
    }

    default double getAsDouble() throws IllegalAttributeTypeException {
        if (getValue() instanceof Double) {
            return (double) getValue();
        } else {
            throw new IllegalAttributeTypeException("the attribute \"" + getKey() + "\" uses a value \"" + getValue().getClass().getName() + "\" and cannot be converted to double.");
        }
    }
    default boolean isDouble() {
        return getValue() instanceof Double;
    }
    default double[] getAsDoubleArray() throws IllegalAttributeTypeException {
        if (getValue() instanceof Double[]) {
            @NotNull Double[] value = (Double[]) getValue();
            double[] array = new double[value.length];

            for (int index = 0; index < value.length; index++) {
                array[index] = value[index];
            }

            return array;
        } else if (getValue() instanceof double[]) {
            return (double[]) getValue();
        } else {
            throw new IllegalAttributeTypeException("the attribute \"" + getKey() + "\" uses a value \"" + getValue().getClass().getName() + "\" and cannot be converted to double array.");
        }
    }
    default boolean isDoubleArray() {
        return getValue() instanceof Double[] || getValue() instanceof double[];
    }

    default float getAsFloat() throws IllegalAttributeTypeException {
        if (getValue() instanceof Float) {
            return (float) getValue();
        } else {
            throw new IllegalAttributeTypeException("the attribute \"" + getKey() + "\" uses a value \"" + getValue().getClass().getName() + "\" and cannot be converted to float.");
        }
    }
    default boolean isFloat() {
        return getValue() instanceof Float;
    }
    default float[] getAsFloatArray() throws IllegalAttributeTypeException {
        if (getValue() instanceof Float[]) {
            @NotNull Float[] value = (Float[]) getValue();
            float[] array = new float[value.length];

            for (int index = 0; index < value.length; index++) {
                array[index] = value[index];
            }

            return array;
        } else if (getValue() instanceof float[]) {
            return (float[]) getValue();
        } else {
            throw new IllegalAttributeTypeException("the attribute \"" + getKey() + "\" uses a value \"" + getValue().getClass().getName() + "\" and cannot be converted to float array.");
        }
    }
    default boolean isFloatArray() {
        return getValue() instanceof Float[] || getValue() instanceof float[];
    }

    default boolean getAsBoolean() throws IllegalAttributeTypeException {
        if (getValue() instanceof Boolean) {
            return (boolean) getValue();
        } else {
            throw new IllegalAttributeTypeException("the attribute \"" + getKey() + "\" uses a value \"" + getValue().getClass().getName() + "\" and cannot be converted to boolean.");
        }
    }
    default boolean isBoolean() {
        return getValue() instanceof Boolean;
    }
    default boolean[] getAsBooleanArray() throws IllegalAttributeTypeException {
        if (getValue() instanceof Boolean[]) {
            @NotNull Boolean[] value = (Boolean[]) getValue();
            boolean[] array = new boolean[value.length];

            for (int index = 0; index < value.length; index++) {
                array[index] = value[index];
            }

            return array;
        } else if (getValue() instanceof boolean[]) {
            return (boolean[]) getValue();
        } else {
            throw new IllegalAttributeTypeException("the attribute \"" + getKey() + "\" uses a value \"" + getValue().getClass().getName() + "\" and cannot be converted to boolean array.");
        }
    }
    default boolean isBooleanArray() {
        return getValue() instanceof Boolean[] || getValue() instanceof boolean[];
    }

    default byte getAsByte() throws IllegalAttributeTypeException {
        if (getValue() instanceof Byte) {
            return (byte) getValue();
        } else {
            throw new IllegalAttributeTypeException("the attribute \"" + getKey() + "\" uses a value \"" + getValue().getClass().getName() + "\" and cannot be converted to byte.");
        }
    }
    default boolean isByte() {
        return getValue() instanceof Byte;
    }
    default byte[] getAsByteArray() throws IllegalAttributeTypeException {
        if (getValue() instanceof Byte[]) {
            @NotNull Byte[] value = (Byte[]) getValue();
            byte[] array = new byte[value.length];

            for (int index = 0; index < value.length; index++) {
                array[index] = value[index];
            }

            return array;
        } else if (getValue() instanceof byte[]) {
            return (byte[]) getValue();
        } else {
            throw new IllegalAttributeTypeException("the attribute \"" + getKey() + "\" uses a value \"" + getValue().getClass().getName() + "\" and cannot be converted to byte array.");
        }
    }
    default boolean isByteArray() {
        return getValue() instanceof Byte[] || getValue() instanceof byte[];
    }

    default short getAsShort() throws IllegalAttributeTypeException {
        if (getValue() instanceof Short) {
            return (short) getValue();
        } else {
            throw new IllegalAttributeTypeException("the attribute \"" + getKey() + "\" uses a value \"" + getValue().getClass().getName() + "\" and cannot be converted to short.");
        }
    }
    default boolean isShort() {
        return getValue() instanceof Short;
    }
    default short[] getAsShortArray() throws IllegalAttributeTypeException {
        if (getValue() instanceof Short[]) {
            @NotNull Short[] value = (Short[]) getValue();
            short[] array = new short[value.length];

            for (int index = 0; index < value.length; index++) {
                array[index] = value[index];
            }

            return array;
        } else if (getValue() instanceof short[]) {
            return (short[]) getValue();
        } else {
            throw new IllegalAttributeTypeException("the attribute \"" + getKey() + "\" uses a value \"" + getValue().getClass().getName() + "\" and cannot be converted to short array.");
        }
    }
    default boolean isShortArray() {
        return getValue() instanceof Short[] || getValue() instanceof short[];
    }

    default char getAsCharacter() throws IllegalAttributeTypeException {
        if (getValue() instanceof Character) {
            return (Character) getValue();
        } else {
            throw new IllegalAttributeTypeException("the attribute \"" + getKey() + "\" uses a value \"" + getValue().getClass().getName() + "\" and cannot be converted to character.");
        }
    }
    default boolean isCharacter() {
        return getValue() instanceof Character;
    }
    default char[] getAsCharacterArray() throws IllegalAttributeTypeException {
        if (getValue() instanceof Character[]) {
            @NotNull Character[] value = (Character[]) getValue();
            char[] array = new char[value.length];

            for (int index = 0; index < value.length; index++) {
                array[index] = value[index];
            }

            return array;
        } else if (getValue() instanceof char[]) {
            return (char[]) getValue();
        } else {
            throw new IllegalAttributeTypeException("the attribute \"" + getKey() + "\" uses a value \"" + getValue().getClass().getName() + "\" and cannot be converted to character array.");
        }
    }
    default boolean isCharacterArray() {
        return getValue() instanceof Character[] || getValue() instanceof char[];
    }

}
