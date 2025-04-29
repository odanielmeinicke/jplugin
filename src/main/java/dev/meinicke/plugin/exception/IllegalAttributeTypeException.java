package dev.meinicke.plugin.exception;

import dev.meinicke.plugin.attribute.AttributeHolder;
import org.jetbrains.annotations.Nullable;

/**
 * Exception thrown to indicate that an attribute retrieved from an {@link AttributeHolder}
 * is of an unexpected or incompatible type.
 * <p>
 * This exception is typically used when an attempt is made to retrieve an attribute value
 * as a specific type (e.g., {@code String}, {@code int[]}, {@code Class<?>}, etc.) but the
 * actual value stored in the attribute does not match the expected type. This type of mismatch
 * may indicate either incorrect attribute usage by a plugin, or an improperly configured or
 * malformed annotation.
 * </p>
 *
 * <p>
 * For example, if an attribute was declared as a {@code int[]} but is retrieved via
 * {@code getAsStringArray()}, this exception will be thrown.
 * </p>
 *
 * <p>
 * This class extends {@link RuntimeException}, meaning it is an unchecked exception and
 * does not need to be declared in method signatures.
 * </p>
 *
 * @see AttributeHolder
 * @author Daniel Meinicke
 * @since 1.1.6
 */
public final class IllegalAttributeTypeException extends RuntimeException {

    /**
     * Constructs a new {@code IllegalAttributeTypeException} with the specified detail message.
     *
     * @param message the detail message, which typically includes information about the
     *                expected and actual attribute types. May be {@code null}.
     */
    public IllegalAttributeTypeException(@Nullable String message) {
        super(message);
    }

    /**
     * Constructs a new {@code IllegalAttributeTypeException} with the specified detail message and cause.
     *
     * @param message the detail message explaining the context of the type mismatch. May be {@code null}.
     * @param cause   the underlying cause of this exception, if available. May be {@code null}.
     */
    public IllegalAttributeTypeException(@Nullable String message, @Nullable Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new {@code IllegalAttributeTypeException} with the specified cause.
     *
     * @param cause the underlying cause of this exception. May be {@code null}.
     */
    public IllegalAttributeTypeException(@Nullable Throwable cause) {
        super(cause);
    }
}