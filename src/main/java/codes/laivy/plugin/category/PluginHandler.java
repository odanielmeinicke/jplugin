package codes.laivy.plugin.category;

import codes.laivy.plugin.exception.PluginInitializeException;
import codes.laivy.plugin.exception.PluginInterruptException;
import codes.laivy.plugin.info.PluginInfo;
import codes.laivy.plugin.info.PluginInfo.State;
import org.jetbrains.annotations.NotNull;

public interface PluginHandler {

    /**
     * Called when a plugin is about to be created, if returned true the plugin will be registered, if false the plugin will not be registered and started
     * @param info
     */
    default boolean accept(@NotNull PluginInfo info) {
        return true;
    }

    /**
     * Called when the plugin state has changed
     * @param info
     */
    default void state(@NotNull PluginInfo info, @NotNull State previous) {
    }

    /**
     * Called when the plugin is now starting, the state is STARTING at this moment
     * @param info
     */
    default void start(@NotNull PluginInfo info) throws PluginInitializeException {
    }

    /**
     * Called when the plugin is now closing, the state is CLOSING at this moment
     * @param info
     */
    default void close(@NotNull PluginInfo info) throws PluginInterruptException {
    }

    /**
     * Called when the plugin is now active and running
     * @param info
     */
    default void run(@NotNull PluginInfo info) {
    }

}
