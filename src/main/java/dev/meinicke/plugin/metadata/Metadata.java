package dev.meinicke.plugin.metadata;

import dev.meinicke.plugin.metadata.type.MetadataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * A {@link Map} implementation with case-insensitive string keys.
 * <p>
 * This class wraps a {@link LinkedHashMap} internally and overrides the standard
 * {@link Map} operations to ensure that all keys are normalized to lower case,
 * allowing for case-insensitive access, insertion, and removal.
 * <p>
 * Important notes:
 * <ul>
 *     <li>Only {@link String} keys are supported; using any other type as a key will cause {@link ClassCastException} or unexpected behavior.</li>
 *     <li>{@code null} keys are not allowed.</li>
 *     <li>The insertion order of entries is preserved.</li>
 *     <li>Accessing a non-existent key via {@link #get(Object)} throws a {@link NullPointerException} with a descriptive message, rather than returning {@code null}.</li>
 * </ul>
 */
public class Metadata extends AbstractMap<String, Object> {

    /**
     * Internal storage map with normalized (lowercase) keys.
     */
    protected final @NotNull Map<String, Object> shade = new LinkedHashMap<>();

    /**
     * Metadata types associated with normalized keys.
     */
    protected final @NotNull Map<String, MetadataType<?>> types = new HashMap<>();

    /**
     * Constructs an empty {@code Metadata} instance.
     */
    public Metadata() {
    }

    // Type operations

    /**
     * Sets the {@link MetadataType} for a specific key.
     * If the key already exists and has a value, the value must be compatible with the provided type.
     *
     * @param key the metadata key to associate with the type.
     * @param type the metadata type definition.
     * @return the previous {@code MetadataType}, or {@code null} if none existed.
     * @throws IllegalStateException if the type is required but the current value is null.
     * @throws IllegalArgumentException if the current value is incompatible with the type.
     */
    public @Nullable MetadataType<?> setType(@NotNull String key, @Nullable MetadataType<?> type) {
        if (type != null) {
            // Retrieve the current value and check if it's compatible with the new metadata type
            @Nullable Object value = shade.getOrDefault(key.toLowerCase(), null);

            // Check if it's not null (if required)
            if (type.isRequired() && value == null) {
                throw new IllegalStateException("the metadata requires a non-null value but the current value is null of metadata key: " + key);
            }

            // Check if reference is assignable
            if (value != null && !type.getReference().isAssignableFrom(value.getClass())) {
                throw new IllegalArgumentException("the current value of metadata key '" + key + "': \"" + value.getClass().getName() + "\" is not assignable with \"" + type.getReference().getName() + "\"");
            }

            // Approve
            //noinspection rawtypes,unchecked
            ((MetadataType) type).accept(value);
        }

        // Finish
        return types.put(key.toLowerCase(), type);
    }

    /**
     * Retrieves the metadata type for the given key, if present.
     *
     * @param key the metadata key to look up.
     * @return an {@link Optional} containing the {@link MetadataType}, or empty if none.
     */
    public @NotNull Optional<MetadataType<?>> getType(@NotNull String key) {
        return Optional.ofNullable(types.getOrDefault(key.toLowerCase(), null));
    }

    /**
     * Returns a set of metadata type entries.
     *
     * @return a set view of all metadata type mappings.
     */
    public @NotNull Set<Entry<String, MetadataType<?>>> typeEntrySet() {
        return types.entrySet();
    }

    // Basic Map Operations

    /**
     * Associates the specified value with the specified key (case-insensitively).
     * If the map previously contained a mapping for the key, the old value is replaced.
     *
     * @param key the key with which the specified value is to be associated (must not be {@code null})
     * @param value the value to be associated with the specified key (may be {@code null})
     * @return the previous value associated with the normalized key, or {@code null} if there was no mapping
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public @Nullable Object put(@NotNull String key, @Nullable Object value) {
        Objects.requireNonNull(key, "key must not be null");

        // Check if the metadata type approves
        @Nullable MetadataType type = types.getOrDefault(key.toLowerCase(), null);

        if (type != null) {
            // Check if it's required
            if (type.isRequired() && value == null) {
                throw new IllegalArgumentException("this metadata value cannot be null!");
            }

            // First check the reference type
            if (value != null && !type.getReference().isAssignableFrom(value.getClass())) {
                throw new IllegalArgumentException("this metadata value must be assignable with \"" + type.getReference().getName() + "\", current is: \"" + value.getClass().getName() + "\"");
            }

            // Approve
            type.accept(value);
        }

        // Finish
        return shade.put(key.toLowerCase(), value);
    }

    /**
     * Removes the mapping for a key (case-insensitively) if it is present.
     *
     * @param key key whose mapping is to be removed from the map
     * @return the previous value associated with the normalized key, or {@code null} if there was no mapping
     */
    @Override
    public @Nullable Object remove(@NotNull Object key) {
        Objects.requireNonNull(key, "key must not be null");
        return shade.remove(key.toString().toLowerCase());
    }

    /**
     * Returns the value to which the specified key (case-insensitively) is mapped.
     *
     * @param key the key whose associated value is to be returned
     * @return the value to which the normalized key is mapped
     */
    @Override
    public @Nullable Object get(@NotNull Object key) {
        return shade.get(key.toString().toLowerCase());
    }

    /**
     * Returns {@code true} if this map contains a mapping for the specified key (case-insensitively).
     *
     * @param key the key whose presence in this map is to be tested
     * @return {@code true} if this map contains a mapping for the normalized key
     */
    @Override
    public boolean containsKey(@NotNull Object key) {
        return shade.containsKey(key.toString().toLowerCase());
    }
    /**
     * Returns a {@link Set} view of the mappings contained in this map.
     * The set is backed by the internal storage map, so changes to the map are reflected in the set.
     *
     * @return a set view of the mappings contained in this map
     */
    @Override
    public @NotNull Set<Entry<String, Object>> entrySet() {
        return shade.entrySet();
    }

    // Typed getters

    /**
     * Retrieves the value associated with the key, cast to a specific type.
     * This method also validates the type against any defined {@link MetadataType}.
     *
     * @param reference the expected class type
     * @param objectKey the metadata key
     * @return the value cast to type {@code T}
     * @param <T> the expected return type
     * @throws NullPointerException if the key does not exist
     * @throws IllegalArgumentException if the value is incompatible with the expected type
     */
    public <T> @Nullable T get(@NotNull Class<T> reference, @NotNull Object objectKey) {
        // Variables
        @NotNull String key = String.valueOf(objectKey);

        // Variables
        @Nullable Object value = shade.get(key.toLowerCase());

        // Check the metadata type if present
        @Nullable MetadataType<?> type = getType(key).orElse(null);

        if (type != null) {
            if (!type.getReference().isAssignableFrom(reference)) {
                throw new IllegalArgumentException("the reference \"" + reference.getName() + "\" cannot be assigned to \"" + type.getReference().getName() + "\" to metadata key: " + key);
            } else if (value != null && reference.isAssignableFrom(value.getClass())) {
                throw new IllegalArgumentException("the metadata value \"" + value.getClass().getName() + "\" cannot be assigned to \"" + reference.getName() + "\" to metadata key: " + key);
            } else if (type.isRequired() && value == null) {
                throw new IllegalArgumentException("the value is null but the metadata type is non-null (required).");
            }
        }

        // Return (with cast)
        //noinspection unchecked
        return (T) value;
    }

    // Nested metadata access

    /**
     * Retrieves a nested {@link Metadata} instance associated with the specified key.
     *
     * @param key the metadata key
     * @return the nested {@code Metadata}
     * @throws NullPointerException if the key does not exist
     * @throws IllegalStateException if the associated value is not a {@code Metadata} instance
     */
    public @NotNull Metadata getMetadata(@NotNull Object key) {
        @Nullable Object value = shade.get(key.toString().toLowerCase());

        if (!(value instanceof Metadata)) {
            throw new IllegalStateException("the metadata key \"" + key + "\" is not a metadata object, current is: " + value);
        }

        // Finish
        return (Metadata) value;
    }

    /**
     * Creates and associates a new empty {@link Metadata} instance to the specified key.
     *
     * @param key the key to bind the metadata to
     * @return the newly created metadata instance
     */
    public @NotNull Metadata setMetadata(@NotNull String key) {
        @NotNull Metadata metadata = new Metadata();
        setMetadata(key, metadata);

        return metadata;
    }
    /**
     * Associates a given {@link Metadata} object to the specified key.
     *
     * @param key the key to associate with
     * @param metadata the metadata object to store
     * @return the previous value associated with the key, or {@code null} if none existed
     */
    public @Nullable Object setMetadata(@NotNull String key, @Nullable Metadata metadata) {
        return put(key, metadata);
    }

    // Merge

    /**
     * Merges all key-value pairs from this {@code Metadata} instance into the target {@code other} instance,
     * replacing any existing keys in the target with values from this one.
     * <p>
     * This operation will overwrite values in {@code other} if a key already exists,
     * using case-insensitive key matching (as is standard for all metadata operations).
     * Schema definitions from {@code other} are preserved.
     *
     * @param other the {@code Metadata} instance to receive all key-value pairs from this metadata.
     * @throws NullPointerException if {@code other} is null.
     */
    public void merge(@NotNull Metadata other) {
        merge(other, true);
    }

    /**
     * Merges all key-value pairs from this {@code Metadata} instance into the target {@code other} instance.
     * <p>
     * This variant gives control over whether to override existing keys in the target.
     * If {@code override} is {@code true}, existing entries in {@code other} will be replaced
     * by values from this instance. If {@code false}, only new keys (not already present in {@code other})
     * will be inserted.
     *
     * <p>
     * Key comparison is case-insensitive, following the standard {@code Metadata} behavior.
     * Schema definitions from {@code other} are preserved.
     *
     * @param other the {@code Metadata} instance to receive the merged values.
     * @param override whether existing keys in {@code other} should be replaced.
     * @throws NullPointerException if {@code other} is null.
     */
    public void merge(@NotNull Metadata other, boolean override) {
        // Merge all types
        for (@NotNull Entry<String, MetadataType<?>> entry : types.entrySet()) {
            @NotNull String key = entry.getKey();
            @NotNull MetadataType<?> type = entry.getValue();

            if (override || !other.types.containsKey(key.toLowerCase())) {
                other.types.put(key.toLowerCase(), type);
            }
        }

        // Merge all values
        for (@NotNull Entry<String, Object> entry : entrySet()) {
            @NotNull String key = entry.getKey();
            @NotNull Object value = entry.getValue();

            if (override || !other.containsKey(key)) {
                other.put(key, value);
            }
        }
    }

    // Implementations

    /**
     * Compares this {@code Metadata} instance with another object for equality.
     * <p>
     * Two {@code Metadata} instances are considered equal if:
     * <ul>
     *     <li>They are the same instance ({@code this == object}), or</li>
     *     <li>The other object is an instance of {@code Metadata}, and</li>
     *     <li>Their parent class (via {@code super.equals(object)}) evaluates to {@code true}, and</li>
     *     <li>Both their {@code shade} and {@code types} maps are equal by {@link Objects#equals(Object, Object)}.</li>
     * </ul>
     *
     * @param object the object to compare for equality
     * @return {@code true} if the given object is equal to this metadata instance, {@code false} otherwise
     */
    @Override
    public boolean equals(@Nullable Object object) {
        if (this == object) return true;
        if (!(object instanceof Metadata)) return false;
        if (!super.equals(object)) return false;
        @NotNull Metadata metadata = (Metadata) object;
        return Objects.equals(shade, metadata.shade) && Objects.equals(types, metadata.types);
    }
    /**
     * Returns the hash code value for this {@code Metadata} instance.
     * <p>
     * The hash code is computed based on:
     * <ul>
     *     <li>The hash code of the superclass ({@code super.hashCode()}),</li>
     *     <li>The internal {@code shade} map (key-value metadata pairs), and</li>
     *     <li>The internal {@code types} map (key-type mappings for schema validation).</li>
     * </ul>
     *
     * @return the hash code value for this metadata object
     */
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), shade, types);
    }

    /**
     * Returns a string representation of this {@code Metadata} instance.
     * <p>
     * The returned string includes the internal state of both {@code shade} and {@code types}
     * maps, formatted as key-value pairs.
     *
     * @return a string representation of this metadata
     */
    @Override
    public @NotNull String toString() {
        return "Metadata{" +
                "shade=" + shade +
                ", types=" + types +
                '}';
    }

}
