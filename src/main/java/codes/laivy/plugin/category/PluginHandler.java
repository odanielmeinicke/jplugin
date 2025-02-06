package codes.laivy.plugin.category;

import codes.laivy.plugin.exception.PluginInitializeException;
import codes.laivy.plugin.exception.PluginInterruptException;
import codes.laivy.plugin.info.PluginInfo;
import org.jetbrains.annotations.NotNull;

/**
 * Defines a set of callback methods for handling lifecycle events and state changes for a plugin.
 * <p>
 * Implementations of this interface can be registered to intercept and respond to various events
 * during a plugin's lifecycle, such as its creation, state transitions, startup, activation, and shutdown.
 * <p>
 * Each method provided by the interface is defined with a default implementation, allowing implementers
 * to override only the callbacks of interest. The methods are invoked in the context of the plugin's lifecycle
 * management, providing the current {@link PluginInfo} instance and, when applicable, additional contextual data.
 */
public interface PluginHandler {

    /**
     * Invoked when a plugin is about to be created.
     * <p>
     * This callback allows the handler to inspect the plugin's metadata and determine whether the plugin should be
     * accepted (i.e., registered and subsequently started) or rejected. A return value of {@code true} indicates that
     * the plugin meets the handler's criteria and will proceed through the lifecycle, while {@code false} will prevent
     * the plugin from being registered and started.
     *
     * @param info The {@link PluginInfo} instance containing metadata and state information about the plugin.
     * @return {@code true} to accept and register the plugin, or {@code false} to reject it.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    default boolean accept(@NotNull PluginInfo info) {
        return true;
    }

    /**
     * Invoked when the plugin's state has changed.
     * <p>
     * This method is called to notify the handler that the plugin has transitioned from a previous state to a new state.
     * Implementers can use this callback to perform custom actions based on state changes, such as logging, monitoring,
     * or triggering additional events.
     *
     * @param info     The {@link PluginInfo} instance representing the plugin whose state has changed.
     * @param previous The previous state of the plugin prior to the change.
     */
    default void state(@NotNull PluginInfo info, @NotNull PluginInfo.State previous) {
    }

    /**
     * Invoked when the plugin is starting.
     * <p>
     * At the time this method is called, the plugin's state has been set to {@code STARTING}. This callback provides an
     * opportunity for handlers to perform any pre-startup logic or to monitor the initialization process. Handlers may
     * throw a {@link PluginInitializeException} to indicate that an error has occurred during startup, which may affect
     * the plugin's overall initialization.
     *
     * @param info The {@link PluginInfo} instance representing the plugin that is starting.
     * @throws PluginInitializeException If an error occurs during the startup process that should prevent the plugin
     *                                   from fully initializing.
     */
    default void start(@NotNull PluginInfo info) throws PluginInitializeException {
    }

    /**
     * Invoked when the plugin is closing.
     * <p>
     * At the time this method is called, the plugin's state is transitioning to a closing phase (typically {@code STOPPING}
     * or similar). This callback allows handlers to execute any necessary cleanup or interruption logic. In the event
     * of an error during the shutdown process, a {@link PluginInterruptException} may be thrown to indicate a problem
     * with the plugin's closure.
     *
     * @param info The {@link PluginInfo} instance representing the plugin that is closing.
     * @throws PluginInterruptException If an error occurs during the shutdown process, preventing a clean closure.
     */
    default void close(@NotNull PluginInfo info) throws PluginInterruptException {
    }

    /**
     * Invoked when the plugin has become active and is now running.
     * <p>
     * This callback signals that the plugin has completed its startup process and is now in the {@code RUNNING} state.
     * Handlers may use this opportunity to perform post-startup actions, such as registering additional services,
     * initializing runtime monitoring, or logging that the plugin is now fully operational.
     *
     * @param info The {@link PluginInfo} instance representing the plugin that is now active and running.
     */
    default void run(@NotNull PluginInfo info) {
    }

}