package codes.laivy.plugin.factory.handlers;

import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.stream.Stream;

/**
 * Defines a collection of {@link PluginHandler} instances that are used to manage and dispatch plugin lifecycle events.
 * <p>
 * The {@code Handlers} interface provides methods to add, remove, and iterate over plugin handlers in a defined order.
 * It is designed to support ordered collections where the order of handlers may influence the sequence in which they
 * are notified of lifecycle events. Handlers can be inserted at specific positions (beginning, end, or at a given index)
 * to ensure precise control over the event handling order.
 * <p>
 * This interface extends {@link Iterable} to allow for-each iteration over the contained {@link PluginHandler} objects.
 * Additionally, it exposes a {@link Stream} interface to enable functional-style operations on the collection of handlers.
 * <p>
 * Implementations of this interface are expected to maintain the insertion order of handlers and support efficient
 * addition, removal, and traversal operations.
 */
public interface Handlers extends Iterable<PluginHandler> {

    /**
     * Creates a new instance of a {@code Handlers} collection.
     * <p>
     * This static factory method returns an implementation of the {@code Handlers} interface.
     * The returned instance is initially empty and can be populated with {@link PluginHandler} objects.
     *
     * @return A new, empty {@code Handlers} instance.
     */
    static @NotNull Handlers create() {
        return new HandlersImpl();
    }

    /**
     * Adds the specified {@link PluginHandler} to this collection.
     * <p>
     * The handler is appended to the end of the collection.
     *
     * @param handler The {@link PluginHandler} to be added. Must not be null.
     * @return {@code true} if the handler was successfully added; {@code false} otherwise.
     */
    boolean add(@NotNull PluginHandler handler);

    /**
     * Inserts the specified {@link PluginHandler} at the specified index in this collection.
     * <p>
     * Handlers at or after the specified index are shifted to the right.
     *
     * @param index   The position at which the handler is to be inserted.
     * @param handler The {@link PluginHandler} to insert. Must not be null.
     */
    void add(int index, @NotNull PluginHandler handler);

    /**
     * Inserts the specified {@link PluginHandler} at the beginning of this collection.
     * <p>
     * This method ensures that the handler will be the first to receive lifecycle event notifications.
     *
     * @param handler The {@link PluginHandler} to add at the beginning. Must not be null.
     */
    void addFirst(@NotNull PluginHandler handler);

    /**
     * Appends the specified {@link PluginHandler} to the end of this collection.
     * <p>
     * This is equivalent to the standard {@link #add(PluginHandler)} method and guarantees that the handler will
     * receive event notifications after any existing handlers.
     *
     * @param handler The {@link PluginHandler} to add at the end. Must not be null.
     */
    void addLast(@NotNull PluginHandler handler);

    /**
     * Removes the specified {@link PluginHandler} from this collection.
     * <p>
     * If the handler is present, it is removed and subsequent handlers are shifted accordingly.
     *
     * @param handler The {@link PluginHandler} to remove. Must not be null.
     * @return {@code true} if the handler was present and removed; {@code false} otherwise.
     */
    boolean remove(@NotNull PluginHandler handler);

    /**
     * Removes all {@link PluginHandler} instances from this collection.
     * <p>
     * After this operation, the collection will be empty.
     */
    void clear();

    /**
     * Returns the number of {@link PluginHandler} instances currently contained in this collection.
     *
     * @return The number of handlers.
     */
    int size();

    /**
     * Determines whether this collection of {@link PluginHandler} instances is empty.
     * <p>
     * This is a convenience method that returns {@code true} if {@link #size()} is zero.
     *
     * @return {@code true} if the collection is empty; {@code false} otherwise.
     */
    default boolean isEmpty() {
        return size() == 0;
    }

    /**
     * Returns an iterator over the {@link PluginHandler} instances contained in this collection.
     * <p>
     * The iterator returns the handlers in the order in which they were added.
     *
     * @return An {@link Iterator} over the plugin handlers.
     */
    @Override
    @NotNull Iterator<PluginHandler> iterator();

    /**
     * Returns a sequential {@link Stream} with this collection as its source.
     * <p>
     * This stream can be used to perform aggregate operations, filtering, mapping, and other functional-style operations on
     * the plugin handlers.
     *
     * @return A {@link Stream} of {@link PluginHandler} instances.
     */
    @NotNull Stream<PluginHandler> stream();
}