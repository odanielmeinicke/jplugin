package codes.laivy.plugin.main;

import codes.laivy.plugin.PluginInfo;
import codes.laivy.plugin.category.PluginCategory;
import codes.laivy.plugin.exception.PluginInitializeException;
import codes.laivy.plugin.exception.PluginInterruptException;
import codes.laivy.plugin.factory.PluginFactory;
import codes.laivy.plugin.factory.PluginFinder;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Optional;

/**
 * The Plugins class serves as a central utility and access point for the plugin framework.
 * <p>
 * This final class provides static methods to interact with the underlying {@link PluginFactory} that
 * manages the lifecycle and discovery of plugins. It exposes methods to retrieve plugin metadata, obtain
 * plugin instances, and perform initialization and interruption operations. All interactions with plugins
 * are delegated to the active PluginFactory instance.
 * <p>
 * The class is designed to be non-instantiable; its constructor is private and throws an UnsupportedOperationException.
 * <p>
 * Upon class loading, a shutdown hook is registered with the runtime to ensure that all plugins are properly interrupted
 * when the application terminates. This guarantees a graceful shutdown of plugin resources.
 * <p>
 * Key functionalities provided by this class include:
 * <ul>
 *   <li>Retrieving the current PluginFactory via {@link #getFactory()} and modifying it using {@link #setFactory(PluginFactory)}.</li>
 *   <li>Accessing plugin finders and categories through methods like {@link #find()} and {@link #getCategory(String)}.</li>
 *   <li>Retrieving plugin metadata and instances using the {@link #retrieve(String)}, {@link #retrieve(Class)} and {@link #getInstance(Class)} methods.</li>
 *   <li>Performing plugin initialization and interruption using a variety of overloaded methods that accept
 *       different parameters such as ClassLoader, package name, and Package object.</li>
 *   <li>Delegating all plugin lifecycle operations (initialize, interrupt, interruptAll, initializeAll) to the PluginFactory.</li>
 * </ul>
 */
public final class Plugins {

    // Static initializers

    static {
        Runtime.getRuntime().addShutdownHook(new ShutdownHook());
    }

    // Factory

    /**
     * The underlying PluginFactory instance used to manage plugin operations.
     */
    private static @NotNull PluginFactory factory = new PluginFactoryImpl();

    /**
     * Returns the current PluginFactory instance.
     *
     * @return The active PluginFactory.
     */
    public static @NotNull PluginFactory getFactory() {
        return factory;
    }

    /**
     * Sets the PluginFactory instance to the provided factory.
     * <p>
     * This allows for customization of the plugin management behavior by replacing the default factory.
     *
     * @param factory The new PluginFactory to use. Must not be null.
     */
    public static void setFactory(@NotNull PluginFactory factory) {
        Plugins.factory = factory;
    }

    /**
     * Returns a PluginFinder to search for plugins based on specified criteria.
     *
     * @return A PluginFinder instance from the current PluginFactory.
     */
    public static @NotNull PluginFinder find() {
        return getFactory().find();
    }

    /**
     * Retrieves the PluginCategory corresponding to the specified name.
     * <p>
     * This method performs a case-insensitive lookup for a PluginCategory with the given name. If a category
     * with that name already exists within the system, the existing instance is returned. Otherwise, if no such
     * PluginCategory exists, a new instance is created using jplugin's default configuration settings.
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
    public static @NotNull PluginCategory getCategory(@NotNull String name) {
        return getFactory().getCategory(name);
    }

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
    public static void setCategory(@NotNull PluginCategory category) {
        getFactory().setCategory(category);
    }

    /**
     * Retrieves the plugin instance associated with the specified class reference, if available.
     *
     * @param reference The Class representing the plugin. Must not be null.
     * @param <T>       The expected type of the plugin instance.
     * @return An Optional containing the plugin instance if present; otherwise, an empty Optional.
     */
    public static <T> @NotNull Optional<T> getInstance(@NotNull Class<?> reference) {
        return getFactory().getInstance(reference);
    }

    /**
     * Retrieves the PluginInfo corresponding to the specified plugin name.
     *
     * @param name The name of the plugin. Must not be null.
     * @return The PluginInfo for the given plugin name.
     */
    public static @NotNull PluginInfo retrieve(@NotNull String name) {
        return getFactory().retrieve(name);
    }

    /**
     * Retrieves the PluginInfo corresponding to the specified plugin class.
     *
     * @param reference The Class representing the plugin. Must not be null.
     * @return The PluginInfo for the given plugin class.
     */
    public static @NotNull PluginInfo retrieve(@NotNull Class<?> reference) {
        return getFactory().retrieve(reference);
    }

    /**
     * Interrupts all plugins loaded by the specified ClassLoader within the given package.
     * <p>
     * The operation will process plugins in the specified package, and if the recursive flag is true,
     * all sub-packages will be included.
     *
     * @param loader    The ClassLoader that loaded the plugins. Must not be null.
     * @param packge    The package name to search for plugins. Must not be null.
     * @param recursive If true, sub-packages are also processed.
     * @throws PluginInterruptException If an error occurs during the interruption process.
     */
    public static void interrupt(@NotNull ClassLoader loader, @NotNull String packge, boolean recursive) throws PluginInterruptException {
        getFactory().interrupt(loader, packge, recursive);
    }

    /**
     * Initializes plugins loaded by the specified ClassLoader within the given package.
     * <p>
     * This method scans the specified package (and sub-packages if recursive is true) for plugins,
     * initializes them, and delegates the operation to the PluginFactory.
     *
     * @param loader    The ClassLoader that loaded the plugins. Must not be null.
     * @param packge    The package name to search for plugins. Must not be null.
     * @param recursive If true, sub-packages are also included.
     * @throws PluginInitializeException If an error occurs during plugin initialization.
     * @throws IOException               If an I/O error occurs during scanning or loading.
     */
    public static void initialize(@NotNull ClassLoader loader, @NotNull String packge, boolean recursive) throws PluginInitializeException, IOException {
        getFactory().initialize(loader, packge, recursive);
    }

    /**
     * Interrupts all plugins within the specified package using the current thread's context ClassLoader.
     *
     * @param packge    The package name to search for plugins. Must not be null.
     * @param recursive If true, sub-packages are also processed.
     * @throws PluginInterruptException If an error occurs during the interruption process.
     */
    public static void interrupt(@NotNull String packge, boolean recursive) throws PluginInterruptException {
        getFactory().interrupt(packge, recursive);
    }

    /**
     * Initializes plugins within the specified package using the current thread's context ClassLoader.
     *
     * @param packge    The package name to search for plugins. Must not be null.
     * @param recursive If true, sub-packages are also included.
     * @throws PluginInitializeException If an error occurs during plugin initialization.
     * @throws IOException               If an I/O error occurs during scanning or loading.
     */
    public static void initialize(@NotNull String packge, boolean recursive) throws PluginInitializeException, IOException {
        getFactory().initialize(packge, recursive);
    }

    /**
     * Interrupts all plugins loaded by the specified ClassLoader within the given Package.
     *
     * @param loader    The ClassLoader that loaded the plugins. Must not be null.
     * @param packge    The Package to search for plugins. Must not be null.
     * @param recursive If true, sub-packages are also processed.
     * @throws PluginInterruptException If an error occurs during the interruption process.
     */
    public static void interrupt(@NotNull ClassLoader loader, @NotNull Package packge, boolean recursive) throws PluginInterruptException {
        getFactory().interrupt(loader, packge, recursive);
    }

    /**
     * Initializes plugins loaded by the specified ClassLoader within the given Package.
     *
     * @param loader    The ClassLoader that loaded the plugins. Must not be null.
     * @param packge    The Package to search for plugins. Must not be null.
     * @param recursive If true, sub-packages are also included.
     * @throws PluginInitializeException If an error occurs during plugin initialization.
     * @throws IOException               If an I/O error occurs during scanning or loading.
     */
    public static void initialize(@NotNull ClassLoader loader, @NotNull Package packge, boolean recursive) throws PluginInitializeException, IOException {
        getFactory().initialize(loader, packge, recursive);
    }

    /**
     * Interrupts all plugins within the specified Package using the current thread's context ClassLoader.
     *
     * @param packge    The Package to search for plugins. Must not be null.
     * @param recursive If true, sub-packages are also processed.
     * @throws PluginInterruptException If an error occurs during the interruption process.
     */
    public static void interrupt(@NotNull Package packge, boolean recursive) throws PluginInterruptException {
        getFactory().interrupt(packge, recursive);
    }

    /**
     * Initializes plugins within the specified Package using the current thread's context ClassLoader.
     *
     * @param packge    The Package to search for plugins. Must not be null.
     * @param recursive If true, sub-packages are also included.
     * @throws PluginInitializeException If an error occurs during plugin initialization.
     * @throws IOException               If an I/O error occurs during scanning or loading.
     */
    public static void initialize(@NotNull Package packge, boolean recursive) throws PluginInitializeException, IOException {
        getFactory().initialize(packge, recursive);
    }

    /**
     * Interrupts all plugins loaded by the specified ClassLoader.
     *
     * @param loader The ClassLoader that loaded the plugins. Must not be null.
     * @throws PluginInterruptException If an error occurs during the interruption process.
     */
    public static void interrupt(@NotNull ClassLoader loader) throws PluginInterruptException {
        getFactory().interrupt(loader);
    }

    /**
     * Experimental method: Initializes all plugins loaded by the specified ClassLoader.
     * <p>
     * This method is marked experimental as its API or behavior may change in future releases.
     *
     * @param loader The ClassLoader that loaded the plugins. Must not be null.
     * @throws PluginInitializeException If an error occurs during plugin initialization.
     * @throws IOException               If an I/O error occurs during scanning or loading.
     */
    @ApiStatus.Experimental
    public static void initialize(@NotNull ClassLoader loader) throws PluginInitializeException, IOException {
        getFactory().initialize(loader);
    }

    /**
     * Experimental method: Initializes all available plugins in the system.
     * <p>
     * This method scans all accessible resources and initializes every plugin found.
     * It is experimental and may change in future releases.
     *
     * @throws PluginInitializeException If an error occurs during plugin initialization.
     * @throws IOException               If an I/O error occurs during scanning or loading.
     */
    @ApiStatus.Experimental
    public static void initializeAll() throws PluginInitializeException, IOException {
        getFactory().initializeAll();
    }

    /**
     * Interrupts all plugins managed by the PluginFactory.
     *
     * @throws PluginInterruptException If an error occurs during the interruption process.
     */
    public static void interruptAll() throws PluginInterruptException {
        getFactory().interruptAll();
    }

    // Private constructor to prevent instantiation

    /**
     * Private constructor to prevent instantiation of the Plugins utility class.
     * <p>
     * This class is not meant to be instantiated and only provides static methods.
     *
     * @throws UnsupportedOperationException Always thrown to prevent instantiation.
     */
    private Plugins() {
        throw new UnsupportedOperationException("this class cannot be instantiated");
    }

    // Inner Classes

    /**
     * A shutdown hook that is registered with the JVM to interrupt all plugins when the application is shutting down.
     * <p>
     * The hook attempts to call {@link #interruptAll()} to ensure that all plugin resources are properly released.
     * Any PluginInterruptException encountered during shutdown is wrapped in a RuntimeException.
     */
    private static final class ShutdownHook extends Thread {

        /**
         * Constructs a new ShutdownHook with a descriptive thread name.
         */
        public ShutdownHook() {
            super("Plug-ins Shutdown Hook");
        }

        /**
         * Invokes the interruption of all plugins during application shutdown.
         * <p>
         * If an exception occurs during the interruption process, it is wrapped in a RuntimeException and thrown.
         */
        @Override
        public void run() {
            try {
                interruptAll();
            } catch (@NotNull PluginInterruptException e) {
                throw new RuntimeException(e);
            }
        }
    }

}