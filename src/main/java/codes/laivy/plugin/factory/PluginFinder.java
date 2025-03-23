package codes.laivy.plugin.factory;

import codes.laivy.plugin.PluginInfo;
import codes.laivy.plugin.category.PluginCategory;
import codes.laivy.plugin.exception.PluginInitializeException;
import codes.laivy.plugin.initializer.PluginInitializer;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.function.Predicate;

/**
 * Provides a fluent API for filtering and retrieving plugins based on a variety of criteria.
 * <p>
 * The PluginFinder interface allows clients to specify detailed constraints such as class loaders, categories,
 * packages, initializers, names, descriptions, dependencies, dependants, instances, and states. These filters are
 * then used to determine which plugins match the given configuration.
 * <p>
 * Clients can chain multiple method calls to narrow down the search criteria. Once configured, the PluginFinder
 * can be used to:
 * <ul>
 *   <li>Check if a given {@link PluginInfo} or plugin class matches the defined criteria using {@link #matches(PluginInfo)}
 *       or {@link #matches(Class)}.</li>
 *   <li>Retrieve an array of {@link PluginInfo} objects representing the plugins that satisfy the filters with {@link #plugins()}.</li>
 *   <li>Retrieve an array of {@code Class<?>} objects corresponding to the matching plugins, even loading unloaded classes if necessary,
 *       via the {@link #classes()} method.</li>
 *   <li>Load the plugins into the system using {@link #load()} or {@link #load(Predicate)}, where additional filtering on the class level
 *       can be applied.</li>
 * </ul>
 * <p>
 * Each filtering method in the interface returns the PluginFinder instance itself, allowing for method chaining.
 */
@SuppressWarnings("UnusedReturnValue")
public interface PluginFinder {

    /**
     * Filters the search to include only plugins loaded by the specified class loaders.
     * <p>
     * All the provided class loaders will be used in the search criteria.
     *
     * @param loaders One or more ClassLoader objects to include.
     * @return This PluginFinder instance with the updated class loader filter.
     */
    @NotNull PluginFinder classLoaders(@NotNull ClassLoader @NotNull ... loaders);

    /**
     * Adds a single ClassLoader to the filter criteria.
     *
     * @param loader The ClassLoader to add.
     * @return This PluginFinder instance with the added ClassLoader.
     */
    @NotNull PluginFinder addClassLoader(@NotNull ClassLoader loader);

    /**
     * Filters the search to include only plugins that belong to the specified categories.
     *
     * @param categories One or more category instances to filter by.
     * @return This PluginFinder instance with the updated category filter.
     */
    @NotNull PluginFinder categories(@NotNull PluginCategory @NotNull ... categories);
    /**
     * Filters the search to include only plugins that belong to the specified categories.
     *
     * @param categories One or more category names to filter by.
     * @return This PluginFinder instance with the updated category filter.
     */
    @NotNull PluginFinder categories(@NotNull String @NotNull ... categories);

    /**
     * Adds a single category to the filter criteria.
     *
     * @param category The category instance to add.
     * @return This PluginFinder instance with the added category.
     */
    @NotNull PluginFinder addCategory(@NotNull PluginCategory category);
    /**
     * Adds a single category to the filter criteria.
     *
     * @param category The category name to add.
     * @return This PluginFinder instance with the added category.
     */
    @NotNull PluginFinder addCategory(@NotNull String category);

    /**
     * Filters the search to include only plugins that are located in the specified packages.
     *
     * @param packages One or more Package objects to filter by.
     * @return This PluginFinder instance with the updated package filter.
     */
    @NotNull PluginFinder packages(@NotNull Package @NotNull ... packages);

    /**
     * Filters the search to include only plugins that are located in the specified packages.
     *
     * @param packages One or more package names to filter by.
     * @return This PluginFinder instance with the updated package filter.
     */
    @NotNull PluginFinder packages(@NotNull String @NotNull ... packages);

    /**
     * Adds a package, specified by its name, to the filter criteria.
     *
     * @param packge The package name to add.
     * @return This PluginFinder instance with the package added.
     */
    @NotNull PluginFinder addPackage(@NotNull String packge);

    /**
     * Adds a package to the filter criteria with an option to include its sub-packages.
     *
     * @param packge    The package name to add.
     * @param recursive If true, sub-packages are also included in the search.
     * @return This PluginFinder instance with the package filter updated.
     */
    @Contract(value = "_,_->this")
    @NotNull PluginFinder addPackage(@NotNull String packge, boolean recursive);

    /**
     * Adds a package, specified as a Package object, to the filter criteria.
     *
     * @param packge The Package to add.
     * @return This PluginFinder instance with the package added.
     */
    @NotNull PluginFinder addPackage(@NotNull Package packge);

    /**
     * Adds a package, specified as a Package object, to the filter criteria with an option to include its sub-packages.
     *
     * @param packge    The Package to add.
     * @param recursive If true, sub-packages are also included.
     * @return This PluginFinder instance with the package filter updated.
     */
    @Contract(value = "_,_->this")
    @NotNull PluginFinder addPackage(@NotNull Package packge, boolean recursive);

    /**
     * Filters the search to include only plugins that are initialized by the specified PluginInitializer classes.
     *
     * @param initializers An array of PluginInitializer classes to filter by.
     * @return This PluginFinder instance with the updated initializer filter.
     */
    @NotNull PluginFinder initializers(@NotNull Class<? extends PluginInitializer> @NotNull [] initializers);

    /**
     * Filters the search to include only plugins that are initialized by the specified PluginInitializer.
     *
     * @param initializer The PluginInitializer class to filter by.
     * @return This PluginFinder instance with the updated initializer filter.
     */
    @NotNull PluginFinder initializer(@NotNull Class<? extends PluginInitializer> initializer);

    /**
     * Adds a PluginInitializer to the filter criteria.
     *
     * @param initializer The PluginInitializer class to add.
     * @return This PluginFinder instance with the initializer added.
     */
    @NotNull PluginFinder addInitializer(@NotNull Class<? extends PluginInitializer> initializer);

    /**
     * Filters the search to include only plugins with the specified names.
     *
     * @param names One or more plugin names to filter by.
     * @return This PluginFinder instance with the updated name filter.
     */
    @NotNull PluginFinder names(@NotNull String @NotNull ... names);

    /**
     * Adds a single plugin name to the filter criteria.
     *
     * @param name The plugin name to add.
     * @return This PluginFinder instance with the name added.
     */
    @NotNull PluginFinder addName(@NotNull String name);

    /**
     * Filters the search to include only plugins with the specified descriptions.
     *
     * @param descriptions One or more plugin descriptions to filter by.
     * @return This PluginFinder instance with the updated description filter.
     */
    @NotNull PluginFinder descriptions(@NotNull String @NotNull ... descriptions);

    /**
     * Adds a plugin description to the filter criteria.
     *
     * @param description The plugin description to add.
     * @return This PluginFinder instance with the description added.
     */
    @NotNull PluginFinder addDescription(@NotNull String description);

    /**
     * Filters the search to include only plugins that depend on the specified classes.
     *
     * @param dependencies One or more Class objects representing dependencies.
     * @return This PluginFinder instance with the updated dependency filter.
     */
    @NotNull PluginFinder dependencies(@NotNull Class<?> @NotNull ... dependencies);

    /**
     * Filters the search to include only plugins that depend on the specified PluginInfo objects.
     *
     * @param dependencies One or more PluginInfo objects representing dependencies.
     * @return This PluginFinder instance with the updated dependency filter.
     */
    @NotNull PluginFinder dependencies(@NotNull PluginInfo @NotNull ... dependencies);

    /**
     * Adds a dependency, specified as a Class object, to the filter criteria.
     *
     * @param dependency The Class representing a dependency to add.
     * @return This PluginFinder instance with the dependency added.
     */
    @NotNull PluginFinder addDependency(@NotNull Class<?> dependency);

    /**
     * Adds a dependency, specified as a PluginInfo object, to the filter criteria.
     *
     * @param dependency The PluginInfo representing a dependency to add.
     * @return This PluginFinder instance with the dependency added.
     */
    @NotNull PluginFinder addDependency(@NotNull PluginInfo dependency);

    /**
     * Filters the search to include only plugins that have the specified dependants, provided as Class objects.
     *
     * @param dependants One or more Class objects representing dependant plugins.
     * @return This PluginFinder instance with the updated dependant filter.
     */
    @NotNull PluginFinder dependants(@NotNull Class<?> @NotNull ... dependants);

    /**
     * Filters the search to include only plugins that have the specified dependants, provided as PluginInfo objects.
     *
     * @param dependants One or more PluginInfo objects representing dependant plugins.
     * @return This PluginFinder instance with the updated dependant filter.
     */
    @NotNull PluginFinder dependants(@NotNull PluginInfo @NotNull ... dependants);

    /**
     * Adds a dependant, specified as a Class object, to the filter criteria.
     *
     * @param dependant The Class representing a dependant plugin.
     * @return This PluginFinder instance with the dependant added.
     */
    @NotNull PluginFinder addDependant(@NotNull Class<?> dependant);

    /**
     * Adds a dependant, specified as a PluginInfo object, to the filter criteria.
     *
     * @param dependant The PluginInfo representing a dependant plugin.
     * @return This PluginFinder instance with the dependant added.
     */
    @NotNull PluginFinder addDependant(@NotNull PluginInfo dependant);

    /**
     * Filters the search to include only plugins whose instances match the specified objects.
     *
     * @param instances One or more objects representing expected plugin instances.
     * @return This PluginFinder instance with the updated instance filter.
     */
    @NotNull PluginFinder instances(@NotNull Object @NotNull ... instances);

    /**
     * Adds an instance to the filter criteria.
     *
     * @param instance The plugin instance to add.
     * @return This PluginFinder instance with the instance filter updated.
     */
    @NotNull PluginFinder addInstance(@NotNull Object instance);

    /**
     * Filters the search to include only plugins that are in one of the specified states.
     *
     * @param states One or more PluginInfo.State values to filter by.
     * @return This PluginFinder instance with the updated state filter.
     */
    @NotNull PluginFinder states(@NotNull PluginInfo.State @NotNull ... states);

    /**
     * Adds a state to the filter criteria.
     *
     * @param state The PluginInfo.State to add.
     * @return This PluginFinder instance with the state filter updated.
     */
    @NotNull PluginFinder addState(@NotNull PluginInfo.State state);

    /**
     * Marks if the plugins should have a shutdown hook to automatically disable them
     *
     * @param shutdownHook true if the plugins should be automatically disabled
     * @return This PluginFinder instance with the state filter updated.
     */
    @NotNull PluginFinder setShutdownHook(boolean shutdownHook);

    /**
     * Determines whether a given {@link PluginInfo} matches the current filter criteria.
     *
     * @param info The PluginInfo object to test.
     * @return {@code true} if the plugin matches the filter; {@code false} otherwise.
     */
    boolean matches(@NotNull PluginInfo info);

    /**
     * Determines whether a plugin identified by the given class reference matches the current filter criteria.
     *
     * @param reference The Class object representing the plugin.
     * @return {@code true} if the plugin matches the filter; {@code false} otherwise.
     */
    boolean matches(@NotNull Class<?> reference);

    /**
     * Retrieves an array of {@link PluginInfo} objects that match the current filter criteria.
     *
     * @return An array of PluginInfo objects satisfying the filter.
     */
    @NotNull PluginInfo @NotNull [] plugins();

    /**
     * Retrieves an array of Class objects corresponding to the plugins that match the current filter criteria.
     * <p>
     * This method scans all classes (including unloaded ones) and analyzes their compatibility with the filter.
     * It may load classes that are not yet loaded if they match the criteria.
     *
     * @return An array of Class objects representing the matching plugins.
     * @throws IOException If an I/O error occurs during class scanning or loading.
     */
    @NotNull Class<?> @NotNull [] classes() throws IOException;

    /**
     * Loads the plugins that match the current filter criteria.
     * <p>
     * This default method loads all matching plugins by applying a predicate that always returns {@code true}.
     *
     * @return An array of PluginInfo objects for the loaded plugins.
     * @throws PluginInitializeException If an error occurs during plugin initialization.
     * @throws IOException               If an I/O error occurs during plugin discovery or loading.
     */
    default @NotNull PluginInfo @NotNull [] load() throws PluginInitializeException, IOException {
        return load((plugin) -> true);
    }

    /**
     * Loads the plugins that match the current filter criteria and satisfy the given predicate.
     * <p>
     * The predicate can be used to perform additional filtering based on the Class object of each plugin.
     *
     * @param predicate A predicate to test each Class object for further filtering.
     * @return An array of PluginInfo objects for the loaded plugins.
     * @throws PluginInitializeException If an error occurs during plugin initialization.
     * @throws IOException               If an I/O error occurs during plugin discovery or loading.
     */
    @NotNull PluginInfo @NotNull [] load(@NotNull Predicate<Class<?>> predicate) throws PluginInitializeException, IOException;

    /**
     * Gets the plugin factory of this plugin finder instance. All the plugin finders must
     * have a plugin factory to specify exactly the factory the plugins will be loaded.
     *
     * @return The plugin factory of this plugin finder instance
     * @since 1.1.5
     */
    @NotNull PluginFactory getFactory();

}