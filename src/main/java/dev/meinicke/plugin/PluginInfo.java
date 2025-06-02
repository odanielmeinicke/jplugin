package dev.meinicke.plugin;

import dev.meinicke.plugin.annotation.Priority;
import dev.meinicke.plugin.category.PluginCategory;
import dev.meinicke.plugin.context.PluginContext;
import dev.meinicke.plugin.exception.PluginInitializeException;
import dev.meinicke.plugin.exception.PluginInterruptException;
import dev.meinicke.plugin.factory.PluginFinder;
import dev.meinicke.plugin.factory.handlers.Handlers;
import dev.meinicke.plugin.factory.handlers.PluginHandler;
import dev.meinicke.plugin.initializer.PluginInitializer;
import dev.meinicke.plugin.main.Plugins;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Represents the core metadata and lifecycle management information for a plugin within the system.
 * <p>
 * Each instance of this abstract class encapsulates a plugin's essential details, including its name,
 * description, associated class reference, dependencies, and lifecycle state. It also manages the relationships
 * between plugins, such as tracking dependants and categorizing the plugin for grouping or filtering purposes.
 * <p>
 * The following key aspects are maintained by this class:
 * <ul>
 *   <li><strong>Plugin Metadata:</strong> Includes an optional name and description that serve as human-readable
 *       identifiers for the plugin. If the name is not provided, the plugin class name is used as a fallback.</li>
 *   <li><strong>Reference Class:</strong> The actual Class object that represents the plugin. This class is expected
 *       to be annotated appropriately (e.g., with a {@code @Plugin} annotation) and is used for reflection-based
 *       operations during plugin initialization and management.</li>
 *   <li><strong>Dependencies and Dependants:</strong> An array of {@link PluginInfo} objects that this plugin depends on,
 *       and a mutable set of plugins that depend on this plugin. These relationships are critical for determining
 *       the order of initialization and shutdown to ensure system stability.</li>
 *   <li><strong>Categories:</strong> A set of strings representing various categories or tags associated with the plugin.
 *       These categories are used for organizing plugins into groups for easier management, extra handling and filtering.</li>
 *   <li><strong>Initializer:</strong> The specific {@link PluginInitializer} implementation class that is responsible
 *       for instantiating and managing the lifecycle of the plugin.</li>
 *   <li><strong>State Management:</strong> A volatile field that tracks the current lifecycle state of the plugin.
 *       The possible states, defined in the inner {@code State} enum, include IDLE, STARTING, RUNNING, STOPPING, and FAILED.
 *   <li><strong>Plugin Instance:</strong> An optional object that represents the active instance of the plugin.
 *       This field may remain null if the plugin does not produce an instance (for example, if only static initialization is performed).</li>
 *   <li><strong>Event Handlers:</strong> A collection of {@link PluginHandler} objects used to notify interested parties
 *       about lifecycle events such as state changes, startup, and shutdown. Handlers can be specific to the plugin,
 *       based on its categories, or global to the entire system.</li>
 * </ul>
 * <p>
 * <strong>Lifecycle Operations:</strong>
 * <ul>
 *   <li>{@code start()}: Transitions the plugin from an idle state to running. This method sets the state to
 *       STARTING, triggers any associated start handlers, and is expected to be called as part of the plugin's
 *       initialization process.</li>
 *   <li>{@code close()}: Initiates the shutdown sequence for the plugin. Prior to shutdown, it verifies that no active
 *       dependant plugins are present. It then sets the state to STOPPING, triggers close handlers, and finally ensures
 *       that the plugin is fully terminated by transitioning the state to IDLE. If the plugin is running or has active
 *       dependants, the shutdown process may be aborted or result in an exception.</li>
 * </ul>
 * <p>
 * <strong>State Enum:</strong>
 * <ul>
 *   <li>{@code IDLE}: Indicates that the plugin is inactive or has been fully stopped.</li>
 *   <li>{@code FAILED}: Signifies that the plugin failed to start. A failure during shutdown results in a normal transition
 *       to IDLE, rather than FAILED.</li>
 *   <li>{@code STARTING}: Represents that the plugin is in the process of initializing.</li>
 *   <li>{@code RUNNING}: Denotes that the plugin is currently active and operational.</li>
 *   <li>{@code STOPPING}: Marks that the plugin is in the process of shutting down.</li>
 * </ul>
 * <p>
 * The {@code PluginInfo} class is a central component in the plugin management framework. It not only stores plugin
 * metadata and handles lifecycle transitions but also ensures that the appropriate event notifications are sent to
 * all registered handlers (including plugin-specific, category-based, and global handlers) during key state changes.
 * <p>
 * This abstract class is typically subclassed by specific implementations that correspond to different plugin initialization
 * strategies (e.g., using constructors, static methods, etc.), ensuring a flexible yet consistent approach to plugin management.
 */
public abstract class PluginInfo {

    // Fields

    /**
     * The optional human-readable name of the plugin. If not provided, the plugin's class name may be used as a fallback.
     */
    private final @Nullable String name;

    /**
     * An optional description providing details about the plugin's functionality.
     */
    private final @Nullable String description;

    /**
     * The class reference of the plugin. This is the actual Class object that represents the plugin and is used for reflection-based operations.
     */
    private final @NotNull Class<?> reference;

    /**
     * An array of PluginInfo objects representing the plugins that this plugin depends on.
     */
    private final @NotNull Collection<PluginInfo> dependencies;

    /**
     * A mutable set of PluginInfo objects representing the plugins that depend on this plugin.
     */
    protected final @NotNull Set<@NotNull PluginInfo> dependants = new LinkedHashSet<>();

    /**
     * A set of category associated with the plugin, used for grouping or filtering.
     */
    private final @NotNull Set<PluginCategory> categories;

    /**
     * The class of the PluginInitializer that is responsible for initializing this plugin.
     */
    private final @NotNull Class<? extends PluginInitializer> initializer;

    /**
     * The loading priority to determine the order which this plugin should be initialized according to others.
     */
    private final int priority;

    /**
     * The current lifecycle state of the plugin. This field is volatile to ensure proper visibility across threads.
     */
    private @NotNull State state = State.IDLE;

    /**
     * The actual plugin instance. This may be null if the plugin initialization strategy does not produce an instance.
     */
    protected @Nullable Object instance;

    /**
     * This boolean field determines if the plugin should be automatically closed with the shutdown hook
     */
    protected boolean autoClose = true;

    /**
     * The collection of event handlers that manage lifecycle events for this plugin.
     */
    private final @NotNull Handlers handlers = Handlers.create();

    /**
     * The main context associated with this plugin, used to retrieve crucial information like metadata and attributes.
     */
    private final @NotNull PluginContext context;

    // Constructor

    /**
     * Constructs a new PluginInfo instance with the specified metadata and lifecycle configuration.
     *
     * @param reference    The Class object representing the plugin.
     * @param name         An optional human-readable name for the plugin.
     * @param description  An optional description of the plugin's functionality.
     * @param dependencies An array of PluginInfo objects that this plugin depends on.
     * @param categories   An array of category names for organizing the plugin.
     * @param initializer  The PluginInitializer class responsible for initializing the plugin.
     * @param priority     The loading priority for this plugin.
     * @param context      The context assigned with this plugin.
     */
    public PluginInfo(@NotNull Class<?> reference, @Nullable String name, @Nullable String description,
                      @NotNull PluginInfo @NotNull [] dependencies, @NotNull PluginCategory @NotNull [] categories,
                      @NotNull Class<? extends PluginInitializer> initializer, int priority, @NotNull PluginContext context) {
        this.name = name;
        this.description = description;
        this.reference = reference;
        this.dependencies = new LinkedHashSet<>(Arrays.asList(dependencies));
        this.categories = new HashSet<>(Arrays.asList(categories));
        this.initializer = initializer;
        this.priority = priority;
        this.context = context;
    }

    // Getters

    /**
     * Returns whether the plugin is configured to be automatically closed during system shutdown.
     * <p>
     * When {@code autoClose} is set to {@code true}, the plugin is expected to be automatically closed by the
     * shutdown hook, allowing it to gracefully release resources and perform necessary cleanup operations.
     * This behavior is integral to ensuring that plugins do not leave open resources or incomplete operations
     * when the application terminates.
     * <p>
     * However, if the shutdown hook mechanism is disabled (for example, via {@link PluginFinder#setShutdownHook(boolean)}
     * with a value of {@code false}), then this flag will have no effect, as the shutdown hook will not trigger
     * the automatic closure of plugins.
     * </p>
     *
     * @return {@code true} if the plugin is configured to be automatically closed on shutdown; {@code false} otherwise.
     */
    public boolean isAutoClose() {
        return autoClose;
    }

    /**
     * Configures whether the plugin should be automatically closed during system shutdown.
     * <p>
     * Setting this flag to {@code true} will enable the plugin to be closed automatically by the shutdown hook,
     * ensuring that it can perform necessary cleanup and resource deallocation when the application is terminating.
     * Conversely, setting it to {@code false} will prevent the plugin from being automatically closed.
     * <p>
     * <strong>Important:</strong> This setting is only effective if the shutdown hook mechanism is enabled.
     * If the shutdown hook is disabled (e.g., by calling {@link PluginFinder#setShutdownHook(boolean)} with {@code false}),
     * then the value of {@code autoClose} will be ignored and the plugin will not be automatically closed on shutdown.
     * </p>
     *
     * @param autoClose {@code true} to enable automatic closure during shutdown; {@code false} to disable it.
     */
    public void setAutoClose(boolean autoClose) {
        this.autoClose = autoClose;
    }

    /**
     * Returns the human-readable name of the plugin.
     *
     * @return The plugin name, or null if not specified.
     */
    public @Nullable String getName() {
        return name;
    }

    /**
     * Returns the description of the plugin's functionality.
     *
     * @return The plugin description, or null if not provided.
     */
    public @Nullable String getDescription() {
        return description;
    }

    /**
     * Returns the current lifecycle state of the plugin.
     *
     * @return The current State of the plugin.
     */
    public final @NotNull State getState() {
        return state;
    }
    protected void setState(@NotNull State state) {
        @NotNull State previous = this.state;
        this.state = state;

        if (previous != state) {
            handle("change state", (handler) -> handler.state(this, previous));

            if (state == State.RUNNING) {
                handle("mark as running", (handler) -> handler.run(this));
            }
        }
    }

    /**
     * Returns the class reference associated with the plugin.
     *
     * @return The Class object representing the plugin.
     */
    public final @NotNull Class<?> getReference() {
        return reference;
    }

    /**
     * Returns a collection of PluginInfo objects representing the plugins that this plugin depends on.
     * Modifying this collection at the runtime may be extremely dangerous to the plugin's loading/unloading.
     *
     * @return A collection of plugin dependencies.
     */
    public @NotNull Collection<@NotNull PluginInfo> getDependencies() {
        return dependencies;
    }

    /**
     * Returns a collection of PluginInfo objects representing the plugins that depend on this plugin.
     *
     * @return A collection of dependant plugins.
     */
    public final @NotNull Collection<@NotNull PluginInfo> getDependants() {
        return dependants;
    }

    /**
     * Returns the set of category associated with the plugin.
     *
     * @return A collection of category strings.
     */
    public @NotNull Collection<PluginCategory> getCategories() {
        return categories;
    }

    /**
     * Returns the PluginInitializer class that is responsible for initializing this plugin.
     *
     * @return The PluginInitializer class.
     */
    public @NotNull Class<? extends PluginInitializer> getInitializer() {
        return initializer;
    }

    /**
     * Returns the numeric priority value associated with this PluginInfo instance.
     * <p>
     * The priority determines the order in which plugins are loaded by the framework.
     * Lower numeric values indicate a higher priority, meaning that a plugin with a lower priority value
     * is loaded before those with higher values.
     * <p>
     * The default priority is 0. This means that if a plugin does not explicitly specify a different priority,
     * it will be considered to have a baseline loading order of 0. Plugins with a priority lower than 0 will be loaded
     * earlier, while those with a priority higher than 0 will be loaded later.
     * <p>
     * It is important to note that the priority value is used in conjunction with dependency resolution:
     * even if a plugin has a high priority (i.e., a lower numeric value), it will still be loaded only after all
     * its dependencies have been successfully resolved.
     * <p>
     * This value can be changed at using the {@link Priority} annotation or at the Builder state using categories or handlers
     *
     * @return an integer representing the plugin's loading priority; lower values denote higher priority.
     */
    public int getPriority() {
        return priority;
    }

    /**
     * Returns the collection of event handlers associated with the plugin.
     *
     * @return The Handlers instance that manages lifecycle events.
     */
    public @NotNull Handlers getHandlers() {
        return handlers;
    }

    /**
     * Returns the instance of the plugin. This instance is created during initialization and may be null if the
     * initialization strategy does not produce an instance (for example, when using a static initializer).
     *
     * @return The plugin instance, or null if no instance exists.
     */
    public final @Nullable Object getInstance() {
        return instance;
    }

    public @NotNull PluginContext getContext() {
        return context;
    }

    // Lifecycle Methods

    /**
     * Initiates the startup process for the plugin.
     * <p>
     * This method transitions the plugin state to STARTING and triggers all associated start handlers.
     * It is expected to be called as part of the overall plugin initialization process.
     *
     * @throws PluginInitializeException If an error occurs during the startup sequence.
     */
    public abstract void start() throws PluginInitializeException;

    /**
     * Initiates the shutdown process for the plugin.
     * <p>
     * This method first verifies that no active dependant plugins are present. If active dependants exist,
     * an {@link IllegalStateException} is thrown. Otherwise, it transitions the plugin state to STOPPING,
     * triggers the close handlers, and completes the shutdown process. The actual resource cleanup should be performed
     * by the specific implementation.
     *
     * @throws PluginInterruptException If an error occurs during the shutdown sequence.
     * @throws IllegalStateException    If active dependant plugins are preventing shutdown.
     */
    public void close() throws PluginInterruptException {
        if (!getState().isRunning()) {
            return;
        }

        // Verify that there are no active dependants preventing shutdown.
        @NotNull PluginInfo[] dependants = getDependants().stream()
                .filter(dependency -> dependency.getState() != State.IDLE && dependency.getState() != State.FAILED)
                .toArray(PluginInfo[]::new);
        if (dependants.length > 0) {
            @NotNull String list = Arrays.toString(dependants);
            list = list.substring(1, list.length() - 1);
            throw new IllegalStateException("cannot interrupt plugin '" + this + "' because there's active dependants: " + list);
        }

        // Transition to stopping state and trigger close handlers.
        setState(State.STOPPING);
    }

    // Equality and String Representation

    /**
     * Determines equality based on the plugin's reference class.
     *
     * @param object The object to compare with this plugin.
     * @return True if the given object is a PluginInfo instance with the same reference class; otherwise, false.
     */
    @Override
    public final boolean equals(@Nullable Object object) {
        if (this == object) return true;
        if (!(object instanceof PluginInfo)) return false;
        @NotNull PluginInfo that = (PluginInfo) object;
        return Objects.equals(getReference(), that.getReference());
    }

    /**
     * Returns the hash code based on the plugin's reference class.
     *
     * @return The hash code of the plugin.
     */
    @Override
    public final int hashCode() {
        return Objects.hashCode(getReference());
    }

    /**
     * Returns a string representation of the plugin, preferring the human-readable name if available.
     *
     * @return The plugin name if defined; otherwise, the plugin's class name.
     */
    @Override
    public final @NotNull String toString() {
        return name != null ? name : getReference().getName();
    }

    // State Enum

    /**
     * Enumerates the possible lifecycle states of a plugin.
     */
    public enum State {

        /**
         * Indicates that the plugin is idle. A plugin may be idle when it has been created, or after it has fully stopped running.
         */
        IDLE,

        /**
         * Indicates that the plugin failed to start. This state is considered an idle state; if the plugin fails to stop,
         * it will transition to IDLE normally.
         */
        FAILED,

        /**
         * Indicates that the plugin is in the process of starting.
         */
        STARTING,

        /**
         * Indicates that the plugin is actively running.
         */
        RUNNING,

        /**
         * Indicates that the plugin is in the process of stopping.
         */
        STOPPING;

        /**
         * Determines if the plugin is currently running.
         *
         * @return True if the state is RUNNING; otherwise, false.
         */
        @SuppressWarnings("BooleanMethodIsAlwaysInverted")
        public boolean isRunning() {
            return this == RUNNING;
        }

        /**
         * Determines if the plugin is idle, which includes both the IDLE and FAILED states.
         *
         * @return True if the plugin is idle; otherwise, false.
         */
        public boolean isIdle() {
            return this == IDLE || this == FAILED;
        }
    }

    // Internal Utility Methods (Private)

    /**
     * Handles lifecycle events by iterating over registered handlers and invoking the specified action.
     * <p>
     * This method dispatches the action to three categories of handlers:
     * <ul>
     *   <li>Plugin-specific handlers registered in this PluginInfo instance.</li>
     *   <li>Category handlers derived from annotations on the plugin class.</li>
     *   <li>Global handlers retrieved from the system-wide plugin factory.</li>
     * </ul>
     *
     * @param action   A descriptive label for the action being performed.
     * @param consumer A consumer that performs the action on each PluginHandler.
     */
    protected void handle(@NotNull String action, @NotNull ThrowingConsumer<PluginHandler> consumer) {
        // Invoke plugin-specific handlers.
        for (@NotNull PluginHandler handler : getHandlers()) {
            try {
                consumer.accept(handler);
            } catch (@NotNull Throwable throwable) {
                throw new RuntimeException("cannot invoke plugin's handler to " + action + " '" + this + "': " + handler, throwable);
            }
        }

        // Invoke category handlers.
        for (@NotNull PluginCategory category : getCategories()) {
            try {
                consumer.accept(category);
            } catch (@NotNull Throwable throwable) {
                throw new RuntimeException("cannot invoke category main handler to " + action + " '" + this + "': " + category, throwable);
            }

            for (@NotNull PluginHandler handler : category.getHandlers()) {
                try {
                    consumer.accept(handler);
                } catch (@NotNull Throwable throwable) {
                    throw new RuntimeException("cannot invoke category's handler list to " + action + " '" + this + "': " + handler, throwable);
                }
            }
        }

        // Global handlers
        for (@NotNull PluginHandler handler : Plugins.getPluginFactory().getGlobalHandlers()) {
            try {
                consumer.accept(handler);
            } catch (@NotNull Throwable throwable) {
                throw new RuntimeException("cannot invoke global list's handler to " + action + " '" + this + "': " + handler, throwable);
            }
        }
    }
    @FunctionalInterface
    protected interface ThrowingConsumer<T> {
        /**
         * Performs this operation on the given argument.
         *
         * @param t the input argument
         * @throws Throwable if any error occurs during the operation
         */
        void accept(T t) throws Throwable;
    }

    // Classes

    private final class Categories extends AbstractSet<PluginCategory> {

        // Object

        private final @NotNull Set<PluginCategory> shade;

        public Categories(@NotNull Set<PluginCategory> shade) {
            this.shade = shade;
        }

        // Modules

        @Override
        public boolean add(@NotNull PluginCategory category) {
            if (shade.contains(category)) {
                return false;
            }

            // Invoke plugin-specific handlers.
            for (@NotNull PluginHandler handler : getHandlers()) {
                try {
                    if (!handler.accept(PluginInfo.this)) {
                        return false;
                    }
                } catch (@NotNull Throwable throwable) {
                    throw new RuntimeException("cannot invoke plugin's handler to add plugin '" + this + "': " + handler, throwable);
                }
            }

            // Invoke category handlers.
            try {
                if (!category.accept(PluginInfo.this)) {
                    return false;
                }
            } catch (@NotNull Throwable throwable) {
                throw new RuntimeException("cannot invoke category main handler to add plugin '" + this + "': " + category, throwable);
            }

            for (@NotNull PluginHandler handler : category.getHandlers()) {
                try {
                    if (!handler.accept(PluginInfo.this)) {
                        return false;
                    }
                } catch (@NotNull Throwable throwable) {
                    throw new RuntimeException("cannot invoke category's handler list to add plugin '" + this + "': " + handler, throwable);
                }
            }

            // Global handlers
            for (@NotNull PluginHandler handler : Plugins.getPluginFactory().getGlobalHandlers()) {
                try {
                    if (!handler.accept(PluginInfo.this)) {
                        return false;
                    }
                } catch (@NotNull Throwable throwable) {
                    throw new RuntimeException("cannot invoke global list's handler to " + handler + " '" + this + "': " + handler, throwable);
                }
            }

            return shade.add(category);
        }

        // Iterators and size

        @Override
        public @NotNull Iterator<@NotNull PluginCategory> iterator() {
            return shade.iterator();
        }
        @Override
        public int size() {
            return shade.size();
        }

    }

}