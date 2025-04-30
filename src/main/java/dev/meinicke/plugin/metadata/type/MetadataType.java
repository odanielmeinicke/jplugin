package dev.meinicke.plugin.metadata.type;

import dev.meinicke.plugin.metadata.Metadata;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Represents a metadata type definition used to validate and describe values associated with metadata keys.
 * <p>
 * This interface is a core component of the {@link Metadata} system, enabling runtime enforcement
 * of type constraints and value semantics for each key. It extends {@link Consumer}, allowing
 * for custom validation logic to be applied when a value is set in a {@code Metadata} instance.
 *
 * <p>
 * Implementations of this interface define the expected type (via {@link #getReference()})
 * and whether the value is required (via {@link #isRequired()}). Validation logic can be provided
 * through the {@link Consumer#accept(Object)} method, which may throw a runtime exception
 * (such as {@link IllegalArgumentException}) if the provided value does not meet the constraints.
 *
 * @param <T> the expected Java type of the metadata value
 */
public interface MetadataType<T> extends Consumer<@Nullable T> {

    /**
     * Returns the {@link Class} object that represents the Java reference type for the metadata value.
     * <p>
     * This is used by the {@link Metadata} container to ensure that any value associated with a given
     * key is type-compatible at runtime. If a value does not match or cannot be assigned to this type,
     * an exception will be thrown during insertion.
     *
     * @return the class object representing the expected type of the metadata value (never {@code null})
     */
    @NotNull Class<T> getReference();

    /**
     * Indicates whether the value associated with this metadata type is required to be non-null.
     * <p>
     * When {@code true}, the {@link Metadata} system will reject {@code null} values for the associated key.
     * This provides a mechanism for enforcing mandatory metadata fields at runtime.
     * <p>
     * The {@link #accept(Object)} method may still perform additional checks on non-null values.
     *
     * @return {@code true} if the value must be non-null, {@code false} if {@code null} is allowed
     */
    boolean isRequired();
}