package codes.laivy.plugin.factory;

import codes.laivy.plugin.PluginInfo;
import codes.laivy.plugin.category.PluginCategory;
import codes.laivy.plugin.exception.PluginInitializeException;
import codes.laivy.plugin.exception.PluginInterruptException;
import codes.laivy.plugin.factory.handlers.Handlers;
import codes.laivy.plugin.factory.handlers.PluginHandler;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Iterator;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * The PluginFactory interface provides a comprehensive API for managing the lifecycle,
 * discovery, and retrieval of plugins within the system.
 * <p>
 * This interface is central to the plugin framework and is responsible for:
 * <ul>
 *   <li>Retrieving plugin metadata by class reference or by a given name.</li>
 *   <li>Managing collections of {@link PluginHandler} objects both globally and for specific categories.</li>
 *   <li>Accessing instantiated plugin objects if available.</li>
 *   <li>Initializing and interrupting plugins based on various contexts, such as ClassLoader, package name, or Package object,
 *       with an option to process recursively through sub-packages.</li>
 *   <li>Providing a {@link PluginFinder} for advanced discovery operations.</li>
 *   <li>Iterating over all registered plugins through the {@link Iterable} interface and functional-style {@link Stream} operations.</li>
 * </ul>
 * <p>
 * The initialization and interruption methods support different input types (ClassLoader, package name, or Package object)
 * to accommodate various runtime environments. Some methods are marked as experimental to indicate that their API or behavior
 * may change in future versions.
 */
@SuppressWarnings("UnusedReturnValue")
public interface PluginFactory extends Iterable<PluginInfo> {

    // Getters

    /**
     * Retrieves the {@link PluginInfo} associated with the specified plugin class reference.
     * <p>
     * This method is used to obtain detailed metadata and lifecycle information for the plugin represented by the given class.
     *
     * @param reference The Class object representing the plugin. Must not be null.
     * @return A non-null {@link PluginInfo} instance corresponding to the provided plugin class.
     */
    @NotNull PluginInfo retrieve(@NotNull Class<?> reference);

    /**
     * Retrieves the {@link PluginInfo} associated with the specified plugin name.
     * <p>
     * This method is useful when the human-readable name of the plugin is known, and it returns the corresponding metadata.
     *
     * @param name The name of the plugin. Must not be null.
     * @return A non-null {@link PluginInfo} instance corresponding to the provided plugin name.
     */
    @NotNull PluginInfo retrieve(@NotNull String name);

    // Global Handler

    /**
     * Retrieves the global handlers list, those handlers will be applied to every plug-ins.
     *
     * @return the global handlers iterable
     */
    @NotNull Handlers getGlobalHandlers();

    // Categories

    /**
     * Retrieves the PluginCategory corresponding to the specified name.
     * <p>
     * This method performs a case-insensitive lookup for a PluginCategory with the given name. If a category
     * with that name already exists within the system, the existing instance is returned. Otherwise, if no such
     * PluginCategory exists, a new instance is created using JPlugin's default configuration settings.
     * <p>
     * The newly created category will automatically include the standard lifecycle event handlers and policies
     * as defined by the framework. This ensures consistency across the system, as each category is uniquely defined
     * and any subsequent calls to this method with the same name (ignoring case) will return the same PluginCategory
     * instance.
     * <p>
     * In summary, this method either returns an existing PluginCategory or creates a new one with default configurations,
     * depending on whether the specified category name has already been registered.
     *
     * @param name the name of the category for which to retrieve the PluginCategory; must not be null. The lookup is case-insensitive.
     * @return a non-null PluginCategory instance corresponding to the specified name, either newly created with default settings or the existing instance.
     */
    @NotNull PluginCategory getCategory(@NotNull String name);

    /**
     * Registers a custom PluginCategory instance.
     * <p>
     * This method allows developers to explicitly set a PluginCategory that they have customized, overriding
     * the default configuration that would otherwise be provided by {@link #getCategory(String)}. When a custom
     * PluginCategory is provided via this method, it is added to the system's category registry, and any subsequent
     * requests for that category name will return the developer-defined instance rather than creating a new one.
     * <p>
     * This mechanism provides flexibility for cases where the default settings are insufficient or when specific
     * customizations are required for a particular group of plugins.
     *
     * @param category the custom PluginCategory instance to register; must not be null.
     */
    void setCategory(@NotNull PluginCategory category);

    /**
     * Retrieves the instance of the plugin corresponding to the given class reference, if it exists.
     * <p>
     * The instance is wrapped in an {@link Optional}. If the plugin has been initialized and an instance is available,
     * the Optional will contain the instance cast to the appropriate type; otherwise, it will be empty.
     *
     * @param reference The Class object representing the plugin. Must not be null.
     * @param <T>       The expected type of the plugin instance.
     * @return An {@link Optional} containing the plugin instance if available; otherwise, an empty Optional.
     */
    <T> @NotNull Optional<T> getInstance(@NotNull Class<?> reference);

    // Initialization and interruption

    /**
     * Interrupts all plugins loaded by the specified ClassLoader within the given package.
     * <p>
     * The interruption process stops plugins and releases associated resources. The {@code recursive} flag determines
     * whether sub-packages should also be processed.
     *
     * @param loader    The ClassLoader used to load the plugins. Must not be null.
     * @param packge    The package name in which to search for plugins. Must not be null.
     * @param recursive If true, plugins in sub-packages will also be interrupted.
     * @throws PluginInterruptException If an error occurs during the interruption process.
     */
    void interrupt(@NotNull ClassLoader loader, @NotNull String packge, boolean recursive) throws PluginInterruptException;

    /**
     * Initializes all plugins loaded by the specified ClassLoader within the given package.
     * <p>
     * The initialization process scans the specified package (and sub-packages if {@code recursive} is true),
     * initializes each discovered plugin, and returns an array of {@link PluginInfo} representing the initialized plugins.
     *
     * @param loader    The ClassLoader used to load the plugins. Must not be null.
     * @param packge    The package name in which to search for plugins. Must not be null.
     * @param recursive If true, plugins in sub-packages will also be initialized.
     * @return An array of non-null {@link PluginInfo} objects corresponding to the initialized plugins.
     * @throws PluginInitializeException If an error occurs during the initialization process.
     * @throws IOException               If an I/O error occurs during plugin discovery or initialization.
     */
    @NotNull PluginInfo @NotNull [] initialize(@NotNull ClassLoader loader, @NotNull String packge, boolean recursive)
            throws PluginInitializeException, IOException;

    /**
     * Interrupts all plugins within the given package using the current thread's context ClassLoader.
     * <p>
     * The {@code recursive} flag determines whether sub-packages should also be processed.
     *
     * @param packge    The package name in which to search for plugins. Must not be null.
     * @param recursive If true, plugins in sub-packages will also be interrupted.
     * @throws PluginInterruptException If an error occurs during the interruption process.
     */
    void interrupt(@NotNull String packge, boolean recursive) throws PluginInterruptException;

    /**
     * Initializes all plugins within the given package using the current thread's context ClassLoader.
     * <p>
     * The {@code recursive} flag determines whether sub-packages should also be processed.
     *
     * @param packge    The package name in which to search for plugins. Must not be null.
     * @param recursive If true, plugins in sub-packages will also be initialized.
     * @return An array of non-null {@link PluginInfo} objects corresponding to the initialized plugins.
     * @throws PluginInitializeException If an error occurs during the initialization process.
     * @throws IOException               If an I/O error occurs during plugin discovery or initialization.
     */
    @NotNull PluginInfo @NotNull [] initialize(@NotNull String packge, boolean recursive)
            throws PluginInitializeException, IOException;

    /**
     * Interrupts all plugins loaded by the specified ClassLoader within the given Package.
     * <p>
     * The {@code recursive} flag determines whether sub-packages should also be processed.
     *
     * @param loader    The ClassLoader used to load the plugins. Must not be null.
     * @param packge    The Package in which to search for plugins. Must not be null.
     * @param recursive If true, plugins in sub-packages will also be interrupted.
     * @throws PluginInterruptException If an error occurs during the interruption process.
     */
    void interrupt(@NotNull ClassLoader loader, @NotNull Package packge, boolean recursive) throws PluginInterruptException;

    /**
     * Initializes all plugins loaded by the specified ClassLoader within the given Package.
     * <p>
     * The {@code recursive} flag determines whether sub-packages should also be processed.
     *
     * @param loader    The ClassLoader used to load the plugins. Must not be null.
     * @param packge    The Package in which to search for plugins. Must not be null.
     * @param recursive If true, plugins in sub-packages will also be initialized.
     * @return An array of non-null {@link PluginInfo} objects corresponding to the initialized plugins.
     * @throws PluginInitializeException If an error occurs during the initialization process.
     * @throws IOException               If an I/O error occurs during plugin discovery or initialization.
     */
    @NotNull PluginInfo @NotNull [] initialize(@NotNull ClassLoader loader, @NotNull Package packge, boolean recursive)
            throws PluginInitializeException, IOException;

    /**
     * Interrupts all plugins within the given Package using the current thread's context ClassLoader.
     * <p>
     * The {@code recursive} flag determines whether sub-packages should also be processed.
     *
     * @param packge    The Package in which to search for plugins. Must not be null.
     * @param recursive If true, plugins in sub-packages will also be interrupted.
     * @throws PluginInterruptException If an error occurs during the interruption process.
     */
    void interrupt(@NotNull Package packge, boolean recursive) throws PluginInterruptException;

    /**
     * Initializes all plugins within the given Package using the current thread's context ClassLoader.
     * <p>
     * The {@code recursive} flag determines whether sub-packages should also be processed.
     *
     * @param packge    The Package in which to search for plugins. Must not be null.
     * @param recursive If true, plugins in sub-packages will also be initialized.
     * @return An array of non-null {@link PluginInfo} objects corresponding to the initialized plugins.
     * @throws PluginInitializeException If an error occurs during the initialization process.
     * @throws IOException               If an I/O error occurs during plugin discovery or initialization.
     */
    @NotNull PluginInfo @NotNull [] initialize(@NotNull Package packge, boolean recursive)
            throws PluginInitializeException, IOException;

    /**
     * Interrupts all plugins loaded by the specified ClassLoader.
     * <p>
     * This method interrupts all plugins that were loaded by the given ClassLoader regardless of their package.
     *
     * @param loader The ClassLoader used to load the plugins. Must not be null.
     * @throws PluginInterruptException If an error occurs during the interruption process.
     */
    void interrupt(@NotNull ClassLoader loader) throws PluginInterruptException;

    /**
     * Initializes all plugins loaded by the specified ClassLoader.
     * <p>
     * This is an experimental method that initializes every plugin available through the given ClassLoader.
     *
     * @param loader The ClassLoader used to load the plugins. Must not be null.
     * @return An array of non-null {@link PluginInfo} objects corresponding to the initialized plugins.
     * @throws PluginInitializeException If an error occurs during the initialization process.
     * @throws IOException               If an I/O error occurs during plugin discovery or initialization.
     */
    @ApiStatus.Experimental
    @NotNull PluginInfo @NotNull [] initialize(@NotNull ClassLoader loader)
            throws PluginInitializeException, IOException;

    /**
     * Initializes all available plugins in the system.
     * <p>
     * This experimental method scans all accessible resources to initialize every plugin present.
     *
     * @return An array of non-null {@link PluginInfo} objects corresponding to the initialized plugins.
     * @throws PluginInitializeException If an error occurs during the initialization process.
     * @throws IOException               If an I/O error occurs during plugin discovery or initialization.
     */
    @ApiStatus.Experimental
    @NotNull PluginInfo @NotNull [] initializeAll() throws PluginInitializeException, IOException;

    /**
     * Interrupts all plugins in the system.
     * <p>
     * This method interrupts every plugin that is currently managed by the PluginFactory, ensuring that all plugin
     * resources are released.
     *
     * @throws PluginInterruptException If an error occurs during the interruption process.
     */
    void interruptAll() throws PluginInterruptException;

    /**
     * Returns a {@link PluginFinder} that can be used to search for plugins using custom criteria.
     * <p>
     * The PluginFinder provides advanced lookup capabilities, enabling the discovery of plugins based on various parameters.
     *
     * @return A non-null {@link PluginFinder} instance for plugin discovery.
     */
    @NotNull PluginFinder find();

    // Plugins

    /**
     * Returns an iterator over all registered {@link PluginInfo} objects.
     * <p>
     * The iterator provides access to the plugins in the order they are managed by the PluginFactory.
     *
     * @return An {@link Iterator} over the plugin metadata objects.
     */
    @Override
    @NotNull Iterator<PluginInfo> iterator();

    /**
     * Returns a sequential {@link Stream} with this PluginFactory as its source.
     * <p>
     * This stream can be used to perform aggregate operations, filtering, mapping, and other functional-style operations
     * on the collection of plugins.
     *
     * @return A {@link Stream} of {@link PluginInfo} objects.
     */
    @NotNull Stream<PluginInfo> stream();
}