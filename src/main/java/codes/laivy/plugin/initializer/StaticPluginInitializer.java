package codes.laivy.plugin.initializer;

import codes.laivy.plugin.exception.PluginInitializeException;
import codes.laivy.plugin.exception.PluginInterruptException;
import codes.laivy.plugin.info.PluginInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A concrete implementation of {@link PluginInitializer} that loads a plugin class
 * without invoking any of its methods or constructors. This is particularly useful
 * for plugins that rely solely on static initialization blocks for setup.
 * <p>
 * This implementation ensures that plugins are initialized in a lightweight manner,
 * avoiding unnecessary instantiation. The internal {@link PluginInfoImpl} class
 * is responsible for managing the plugin's lifecycle, including state transitions
 * and error handling.
 * </p>
 */
public final class StaticPluginInitializer implements PluginInitializer {

    // Object

    /**
     * Private constructor to prevent illegal instantiation.
     * This class is designed to be used by reflections.
     */
    private StaticPluginInitializer() {
    }

    // Modules

    /**
     * Creates a {@link PluginInfo} instance associated with the given plugin class.
     * This method does not instantiate the plugin class but instead registers it
     * for lifecycle management.
     *
     * @param reference    The class annotated with @Plugin.
     * @param name         The plugin's name (nullable).
     * @param description  The plugin's description (nullable).
     * @param dependencies An array of required plugin dependencies.
     * @param categories   An array of category tags associated with the plugin.
     * @return A {@link PluginInfo} instance containing the plugin's metadata and lifecycle management logic.
     */
    @Override
    public @NotNull PluginInfo create(@NotNull Class<?> reference, @Nullable String name, @Nullable String description, @NotNull PluginInfo @NotNull [] dependencies, @NotNull String @NotNull [] categories) {
        return new PluginInfoImpl(reference, name, description, dependencies, categories);
    }

    // Classes

    /**
     * A private implementation of {@link PluginInfo} tailored for {@link StaticPluginInitializer}.
     * It manages the plugin's lifecycle, including startup and shutdown, while ensuring
     * proper state transitions and error handling.
     */
    private static final class PluginInfoImpl extends PluginInfo {

        // Object

        /**
         * Constructs a new {@link PluginInfoImpl} instance, linking it to the
         * {@link StaticPluginInitializer} and setting the initial plugin metadata.
         *
         * @param reference    The plugin class reference.
         * @param name         The plugin name (nullable).
         * @param description  The plugin description (nullable).
         * @param dependencies An array of plugin dependencies.
         * @param categories   An array of category tags.
         */
        public PluginInfoImpl(@NotNull Class<?> reference, @Nullable String name, @Nullable String description, @NotNull PluginInfo @NotNull [] dependencies, @NotNull String @NotNull [] categories) {
            super(reference, name, description, dependencies, categories, StaticPluginInitializer.class);
        }

        // Modules

        /**
         * Starts the plugin lifecycle. This method marks the plugin as running
         * and ensures that any initialization errors are properly handled.
         *
         * @throws PluginInitializeException If an error occurs during initialization.
         */
        @Override
        public void start() throws PluginInitializeException {
            try {
                super.start();
            } catch (@NotNull Throwable throwable) {
                setState(State.FAILED);
                throw new RuntimeException("cannot initialize plugin: " + this, throwable);
            }

            // Mark as running
            setState(State.RUNNING);
        }

        /**
         * Closes the plugin, ensuring that resources are released and
         * transitioning the state back to {@link State#IDLE}.
         *
         * @throws PluginInterruptException If an error occurs during shutdown.
         */
        @Override
        public void close() throws PluginInterruptException {
            if (!getState().isRunning()) {
                return;
            }

            try {
                super.close();
            } finally {
                instance = null;
                setState(State.IDLE);
            }
        }

    }

}
