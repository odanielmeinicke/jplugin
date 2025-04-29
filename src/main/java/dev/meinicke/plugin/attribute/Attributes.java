package dev.meinicke.plugin.attribute;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Stream;

/**
 * A non-thread-safe, ordered collection of unique {@link AttributeHolder} instances,
 * each representing a strongly-typed key-value pair.
 * <p>
 * The {@code Attributes} class allows safe addition, removal, and retrieval of
 * attribute metadata by key or entire attribute instance. Keys are case-insensitive
 * and must be unique within the collection. Adding an attribute with a duplicate key
 * (ignoring case) will be rejected.
 * </p>
 *
 * <p>
 * This class provides overloaded {@code put(...)} methods to insert attributes
 * for a variety of primitive types, boxed types, arrays, {@code String}s, and {@code Class}
 * objects, abstracting the underlying {@link AttributeHolder} creation process.
 * </p>
 *
 * <p><strong>Important:</strong> This class is not thread-safe and must be externally synchronized
 * if used from multiple threads concurrently.</p>
 *
 * <h2>Key Behavior</h2>
 * <ul>
 *   <li>Keys are case-insensitive for all internal operations (insertion, lookup, removal).</li>
 *   <li>Attempting to add two attributes with logically equal keys but different casing will reject the second.</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * Attributes attributes = new Attributes();
 * attributes.put("size", 42);
 * attributes.put("type", "String");
 *
 * Optional<AttributeHolder> sizeAttr = attributes.getByKey("SIZE");
 * }</pre>
 *
 * @see AttributeHolder
 * @author Daniel Meinicke
 * @since 1.1.6
 */
public class Attributes implements Iterable<AttributeHolder> {

    /**
     * The internal set holding all registered attributes in insertion order.
     * Keys are case-insensitively unique.
     */
    protected final @NotNull Set<AttributeHolder> attributes = new LinkedHashSet<>();

    /**
     * Checks if the collection contains an attribute that matches the given {@link AttributeHolder}
     * by key (case-insensitive).
     *
     * @param attribute the attribute to check for existence
     * @return {@code true} if an attribute with the same key is already present, {@code false} otherwise
     */
    public boolean contains(@NotNull AttributeHolder attribute) {
        return attributes.contains(attribute) || contains(attribute.getKey());
    }

    /**
     * Checks if an attribute with the given key exists in the collection.
     * The check is case-insensitive.
     *
     * @param key the key of the attribute to check
     * @return {@code true} if an attribute with that key exists, {@code false} otherwise
     */
    public boolean contains(@NotNull String key) {
        return attributes.stream().anyMatch(target -> target.getKey().equalsIgnoreCase(key));
    }

    /**
     * Retrieves the first attribute associated with the specified key (case-insensitive).
     *
     * @param key the key of the attribute
     * @return an {@link Optional} containing the attribute if found, or empty otherwise
     */
    public @NotNull Optional<AttributeHolder> getByKey(@NotNull String key) {
        return attributes.stream()
                .filter(attribute -> attribute.getKey().equalsIgnoreCase(key))
                .findFirst();
    }

    /**
     * Adds a new {@link AttributeHolder} to this collection.
     * If an attribute with the same key already exists (case-insensitive), it will not be added.
     *
     * @param attribute the attribute to add
     * @return {@code true} if added successfully, {@code false} if an attribute with the same key exists
     */
    public boolean add(@NotNull AttributeHolder attribute) {
        if (contains(attribute)) {
            return false;
        } else {
            return attributes.add(attribute);
        }
    }

    /**
     * Removes an attribute by its key (case-insensitive).
     *
     * @param attribute the attribute to remove (only the key is used)
     * @return {@code true} if an attribute with the given key was found and removed, {@code false} otherwise
     */
    public boolean remove(@NotNull AttributeHolder attribute) {
        return attributes.removeIf(att -> att.getKey().equalsIgnoreCase(attribute.getKey()));
    }

    // Overloaded put methods for various types — documented as a block to avoid redundancy.

    /**
     * Associates the specified key with a new {@link AttributeHolder} wrapping the provided value.
     * The value may be of any supported type (e.g., primitives, arrays, {@code String}, {@code Class}, boxed types).
     * <p>
     * These methods return {@code false} if an attribute with the same key already exists.
     * </p>
     *
     * @param key   the attribute key (case-insensitive)
     * @param value the value to associate with the key
     * @return {@code true} if the attribute was successfully added, {@code false} otherwise
     */
    public boolean put(@NotNull String key, @NotNull String value) { return add(new AttributeHolderImpl(key, value)); }
    /**
     * @see #put(String, String)
     */
    public boolean put(@NotNull String key, @NotNull String[] value) { return add(new AttributeHolderImpl(key, value)); }

    /**
     * @see #put(String, String)
     */
    public boolean put(@NotNull String key, @NotNull Class<?> value) { return add(new AttributeHolderImpl(key, value)); }
    /**
     * @see #put(String, String)
     */
    public boolean put(@NotNull String key, @NotNull Class<?>[] value) { return add(new AttributeHolderImpl(key, value)); }

    /**
     * @see #put(String, String)
     */
    public boolean put(@NotNull String key, int value) { return add(new AttributeHolderImpl(key, value)); }
    /**
     * @see #put(String, String)
     */
    public boolean put(@NotNull String key, int @NotNull [] value) { return add(new AttributeHolderImpl(key, value)); }
    /**
     * @see #put(String, String)
     */
    public boolean put(@NotNull String key, @NotNull Integer @NotNull [] value) { return add(new AttributeHolderImpl(key, value)); }

    /**
     * @see #put(String, String)
     */
    public boolean put(@NotNull String key, long value) { return add(new AttributeHolderImpl(key, value)); }
    /**
     * @see #put(String, String)
     */
    public boolean put(@NotNull String key, long @NotNull [] value) { return add(new AttributeHolderImpl(key, value)); }
    /**
     * @see #put(String, String)
     */
    public boolean put(@NotNull String key, @NotNull Long @NotNull [] value) { return add(new AttributeHolderImpl(key, value)); }

    /**
     * @see #put(String, String)
     */
    public boolean put(@NotNull String key, double value) { return add(new AttributeHolderImpl(key, value)); }
    /**
     * @see #put(String, String)
     */
    public boolean put(@NotNull String key, double @NotNull [] value) { return add(new AttributeHolderImpl(key, value)); }
    /**
     * @see #put(String, String)
     */
    public boolean put(@NotNull String key, @NotNull Double @NotNull [] value) { return add(new AttributeHolderImpl(key, value)); }

    /**
     * @see #put(String, String)
     */
    public boolean put(@NotNull String key, float value) { return add(new AttributeHolderImpl(key, value)); }
    /**
     * @see #put(String, String)
     */
    public boolean put(@NotNull String key, float @NotNull [] value) { return add(new AttributeHolderImpl(key, value)); }
    /**
     * @see #put(String, String)
     */
    public boolean put(@NotNull String key, @NotNull Float @NotNull [] value) { return add(new AttributeHolderImpl(key, value)); }

    /**
     * @see #put(String, String)
     */
    public boolean put(@NotNull String key, boolean value) { return add(new AttributeHolderImpl(key, value)); }
    /**
     * @see #put(String, String)
     */
    public boolean put(@NotNull String key, boolean @NotNull [] value) { return add(new AttributeHolderImpl(key, value)); }
    /**
     * @see #put(String, String)
     */
    public boolean put(@NotNull String key, @NotNull Boolean @NotNull [] value) { return add(new AttributeHolderImpl(key, value)); }

    /**
     * @see #put(String, String)
     */
    public boolean put(@NotNull String key, byte value) { return add(new AttributeHolderImpl(key, value)); }
    /**
     * @see #put(String, String)
     */
    public boolean put(@NotNull String key, byte @NotNull [] value) { return add(new AttributeHolderImpl(key, value)); }
    /**
     * @see #put(String, String)
     */
    public boolean put(@NotNull String key, @NotNull Byte @NotNull [] value) { return add(new AttributeHolderImpl(key, value)); }

    /**
     * @see #put(String, String)
     */
    public boolean put(@NotNull String key, short value) { return add(new AttributeHolderImpl(key, value)); }
    /**
     * @see #put(String, String)
     */
    public boolean put(@NotNull String key, short @NotNull [] value) { return add(new AttributeHolderImpl(key, value)); }
    /**
     * @see #put(String, String)
     */
    public boolean put(@NotNull String key, @NotNull Short @NotNull [] value) { return add(new AttributeHolderImpl(key, value)); }

    /**
     * @see #put(String, String)
     */
    public boolean put(@NotNull String key, char value) { return add(new AttributeHolderImpl(key, value)); }
    /**
     * @see #put(String, String)
     */
    public boolean put(@NotNull String key, char @NotNull [] value) { return add(new AttributeHolderImpl(key, value)); }
    /**
     * @see #put(String, String)
     */
    public boolean put(@NotNull String key, @NotNull Character @NotNull [] value) { return add(new AttributeHolderImpl(key, value)); }

    /**
     * Returns an iterator over the {@link AttributeHolder}s contained in this collection.
     * The iteration order is the insertion order.
     *
     * @return an iterator for attribute traversal
     */
    @Override
    public @NotNull Iterator<AttributeHolder> iterator() {
        return attributes.iterator();
    }

    /**
     * Returns a sequential {@link Stream} of all {@link AttributeHolder}s.
     * Useful for filtering, mapping, and collecting attribute metadata.
     *
     * @return a stream over the attribute holders
     */
    public @NotNull Stream<AttributeHolder> stream() {
        return attributes.stream();
    }

    // Classes

    private static final class AttributeHolderImpl implements AttributeHolder {

        // Object

        private final @NotNull String key;
        private final @NotNull Object value;

        public AttributeHolderImpl(@NotNull String key, @NotNull Object value) {
            this.key = key;
            this.value = value;

            // Verifications
            if (!key.matches("^[^\"'\\\\\\x00-\\x1F\\x7F]+$")) {
                throw new IllegalArgumentException("this attribute key contains illegal characters");
            }
        }

        // Getters

        @Override
        public @NotNull String getKey() {
            return key;
        }
        @Override
        public @NotNull Object getValue() {
            return value;
        }

        // Implementations

        @Override
        public boolean equals(@Nullable Object object) {
            if (this == object) return true;
            if (!(object instanceof AttributeHolderImpl)) return false;
            @NotNull AttributeHolderImpl that = (AttributeHolderImpl) object;
            return getKey().equalsIgnoreCase(that.getKey());
        }
        @Override
        public int hashCode() {
            return Objects.hashCode(getKey().toLowerCase());
        }

        @Override
        public @NotNull String toString() {
            return getKey() + "=" + getValue();
        }

    }

}
