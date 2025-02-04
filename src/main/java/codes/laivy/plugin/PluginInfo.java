package codes.laivy.plugin;

import codes.laivy.plugin.exception.PluginInitializeException;
import codes.laivy.plugin.exception.PluginInterruptException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public interface PluginInfo {

    @Nullable String getName();
    @NotNull State getState();

    @NotNull Class<?> getReference();

    @NotNull Collection<@NotNull PluginInfo> getDependencies();
    @NotNull Collection<@NotNull PluginInfo> getDependants();

    // Modules

    void start() throws PluginInitializeException;
    void close() throws PluginInterruptException;

    // Classes

    enum State {

        IDLE,
        STARTING,
        RUNNING,
        STOPPING,

        FAILED,
        ;

    }

}
