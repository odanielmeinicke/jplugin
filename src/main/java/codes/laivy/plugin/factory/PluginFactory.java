package codes.laivy.plugin.factory;

import codes.laivy.plugin.exception.PluginInitializeException;
import codes.laivy.plugin.exception.PluginInterruptException;
import codes.laivy.plugin.factory.handlers.Handlers;
import codes.laivy.plugin.info.PluginInfo;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Iterator;
import java.util.Optional;
import java.util.stream.Stream;

public interface PluginFactory extends Iterable<PluginInfo> {

    // Getters

    @NotNull PluginInfo retrieve(@NotNull Class<?> reference);
    @NotNull PluginInfo retrieve(@NotNull String name);

    // Categories

    @NotNull Handlers getHandlers();
    @NotNull Handlers getHandlers(@NotNull String category);

    <T> @NotNull Optional<T> getInstance(@NotNull Class<?> reference);

    // Initialization and interruption

    void interrupt(@NotNull ClassLoader loader, @NotNull String packge, boolean recursive) throws PluginInterruptException;
    void initialize(@NotNull ClassLoader loader, @NotNull String packge, boolean recursive) throws PluginInitializeException, IOException;

    void interrupt(@NotNull String packge, boolean recursive) throws PluginInterruptException;
    void initialize(@NotNull String packge, boolean recursive) throws PluginInitializeException, IOException;

    void interrupt(@NotNull ClassLoader loader, @NotNull Package packge, boolean recursive) throws PluginInterruptException;
    void initialize(@NotNull ClassLoader loader, @NotNull Package packge, boolean recursive) throws PluginInitializeException, IOException;

    void interrupt(@NotNull Package packge, boolean recursive) throws PluginInterruptException;
    void initialize(@NotNull Package packge, boolean recursive) throws PluginInitializeException, IOException;

    void interrupt(@NotNull ClassLoader loader) throws PluginInterruptException;
    @ApiStatus.Experimental
    void initialize(@NotNull ClassLoader loader) throws PluginInitializeException, IOException;

    @ApiStatus.Experimental
    void initializeAll() throws PluginInitializeException, IOException;
    void interruptAll() throws PluginInterruptException;

    // Plugins

    @Override
    @NotNull Iterator<PluginInfo> iterator();
    @NotNull Stream<PluginInfo> stream();

}
