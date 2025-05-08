package dev.meinicke.plugin.factory;

import dev.meinicke.plugin.initializer.ConstructorPluginInitializer;
import dev.meinicke.plugin.initializer.MethodPluginInitializer;
import dev.meinicke.plugin.initializer.PluginInitializer;
import dev.meinicke.plugin.initializer.StaticPluginInitializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;

/**
 * Responsible for providing, caching, and managing {@link PluginInitializer} instances used by the
 * framework to construct and configure plugins at runtime. An {@code InitializerFactory} abstracts
 * the discovery, instantiation, caching, and lifecycle of all available {@link PluginInitializer}
 * implementations, ensuring that each initializer type is created only once (unless a custom
 * strategy is configured) and reused for subsequent plugin loads.
 * <p>
 * This interface also extends {@link Closeable} to allow graceful shutdown: once closed,
 * all cached initializer instances are purged and any further access will result in
 * {@link IllegalStateException}, preventing accidental reuse of stale or invalid initializers.
 * </p>
 *
 * <h3>Key Responsibilities</h3>
 * <ul>
 *   <li><b>Discovery:</b> Identify all {@link PluginInitializer} implementations on the classpath
 *       (including built-in and user-provided custom initializers).</li>
 *   <li><b>Instantiation:</b> Reflectively construct initializer instances, honoring visibility
 *       and constructor requirements, then optionally cache them for reuse.</li>
 *   <li><b>Caching and Performance:</b> Maintain a per-type cache to avoid repeated reflective
 *       instantiation overhead. Clearing the cache on {@link #close()} frees resources.</li>
 *   <li><b>Type Safety:</b> Provide type-safe access via {@link #getInitializer(Class)}, ensuring
 *       the returned object is an instance of the requested {@link PluginInitializer} subclass.</li>
 *   <li><b>Lifecycle Management:</b> Enforce that after {@link #close()}, all methods throw
 *       {@link IllegalStateException}, guaranteeing that no further initializers can be fetched
 *       from a closed factory.</li>
 *   <li><b>Extensibility:</b> Allow custom factory implementations to override discovery, caching,
 *       instantiation, or shutdown behavior (e.g., injecting dependencies into initializers).</li>
 * </ul>
 *
 * <h3>Thread Safety</h3>
 * Implementations should be thread-safe: concurrent calls to {@link #getInitializers()},
 * {@link #getInitializer(Class)}, and {@link #close()} must not result in race conditions or
 * inconsistent cache state. Synchronization or concurrent collections are recommended.
 *
 * <h3>Close Behavior</h3>
 * Invoking {@link #close()} on the factory performs these actions:
 * <ol>
 *   <li>Clears the internal cache of all {@link PluginInitializer} instances.</li>
 *   <li>Releases any auxiliary resources held by the factory (e.g., reflection caches).</li>
 *   <li>Transitions the factory into a closed state where any subsequent method call
 *       ({@link #getInitializers} or {@link #getInitializer}) results in
 *       {@link IllegalStateException}.</li>
 * </ol>
 * This ensures that after shutdown, no stale initializers linger and resources are fully freed.
 *
 * @see PluginInitializer
 * @see java.io.Closeable
 * @see ConstructorPluginInitializer
 * @see MethodPluginInitializer
 * @see StaticPluginInitializer
 */
public interface InitializerFactory extends Closeable {

    // Getters

    /**
     * Returns an unmodifiable collection of all {@link PluginInitializer} instances managed
     * by this factory.
     * <p>
     * Each element is non-null and implements {@link PluginInitializer}. The returned
     * collection is unmodifiable—attempts to modify it throw {@link UnsupportedOperationException}.
     * </p>
     *
     * @return an unmodifiable {@code Collection<PluginInitializer>} of all available initializers
     * @throws IllegalStateException if the factory is closed or if discovery/instantiation fails
     */
    @Unmodifiable
    @NotNull Collection<@NotNull PluginInitializer> getInitializers();

    /**
     * Returns the cached instance of the specified initializer class, creating and caching
     * it if it does not already exist.
     * <p>
     * Subsequent calls with the same {@code reference} return the same cached instance,
     * until {@link #close()} is invoked.
     * </p>
     *
     * @param <T>        the concrete {@link PluginInitializer} type
     * @param reference  the class object of the initializer to retrieve
     * @return a non-null instance of {@code reference} implementing {@link PluginInitializer}
     * @throws NullPointerException     if {@code reference} is null
     * @throws IllegalArgumentException if {@code reference} does not implement {@link PluginInitializer}
     * @throws IllegalStateException    if the factory is closed or instantiation fails
     */
    <T extends PluginInitializer> @NotNull T getInitializer(@NotNull Class<T> reference);

    // Modules

    /**
     * Closes this factory, clearing its internal cache and releasing resources.
     * After {@code close()} returns, any call to {@link #getInitializers()} or
     * {@link #getInitializer(Class)} will throw {@link IllegalStateException}.
     *
     * @throws IOException             if an I/O error occurs during resource release
     * @throws IllegalStateException   if the factory is already closed
     */
    @Override
    void close() throws IOException;

}