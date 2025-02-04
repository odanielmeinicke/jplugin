package codes.laivy.plugin.info;

import codes.laivy.plugin.annotation.Initializer;
import codes.laivy.plugin.exception.PluginInitializeException;
import codes.laivy.plugin.exception.PluginInterruptException;
import codes.laivy.plugin.loader.ConstructorPluginLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public interface PluginInfo {

    @Nullable String getName();
    @NotNull State getState();

    @NotNull Class<?> getReference();

    @NotNull Collection<@NotNull PluginInfo> getDependencies();
    @NotNull Collection<@NotNull PluginInfo> getDependants();

    /**
     * Represents the instance of this plugin, the instance of the reference. It could be
     * null if the {@link Initializer} uses a PluginLoader that generates the instance like {@link ConstructorPluginLoader}
     *
     * @return
     */
    @Nullable Object getInstance();

    // Modules

    void start() throws PluginInitializeException;
    void close() throws PluginInterruptException;

    // Classes

    enum State {

        IDLE,
        FAILED,

        STARTING,
        RUNNING,
        STOPPING,
        ;

        public boolean isRunning() {
            return this == RUNNING;
        }
        public boolean isIdle() {
            return this == IDLE || this == FAILED;
        }

    }

}
