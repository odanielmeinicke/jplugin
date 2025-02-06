package codes.laivy.plugin.main;

import codes.laivy.plugin.exception.PluginInitializeException;
import codes.laivy.plugin.exception.PluginInterruptException;
import codes.laivy.plugin.factory.PluginFactory;
import codes.laivy.plugin.factory.PluginFinder;
import codes.laivy.plugin.factory.handlers.Handlers;
import codes.laivy.plugin.info.PluginInfo;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Optional;

public final class Plugins {

    // Static initializers

    static {
        Runtime.getRuntime().addShutdownHook(new ShutdownHook());
    }

    // Factory

    private static @NotNull PluginFactory factory = new PluginFactoryImpl();

    public static @NotNull PluginFactory getFactory() {
        return factory;
    }
    public static void setFactory(@NotNull PluginFactory factory) {
        Plugins.factory = factory;
    }

    public static @NotNull PluginFinder find() {
        return getFactory().find();
    }

    public static @NotNull Handlers getHandlers() {
        return getFactory().getHandlers();
    }
    public static @NotNull Handlers getHandlers(@NotNull String category) {
        return getFactory().getHandlers(category);
    }

    public static <T> @NotNull Optional<T> getInstance(@NotNull Class<?> reference) {
        return getFactory().getInstance(reference);
    }

    public static @NotNull PluginInfo retrieve(@NotNull String name) {
        return getFactory().retrieve(name);
    }
    public static @NotNull PluginInfo retrieve(@NotNull Class<?> reference) {
        return getFactory().retrieve(reference);
    }

    public static void interrupt(@NotNull ClassLoader loader, @NotNull String packge, boolean recursive) throws PluginInterruptException {
        getFactory().interrupt(loader, packge, recursive);
    }
    public static void initialize(@NotNull ClassLoader loader, @NotNull String packge, boolean recursive) throws PluginInitializeException, IOException {
        getFactory().initialize(loader, packge, recursive);
    }

    public static void interrupt(@NotNull String packge, boolean recursive) throws PluginInterruptException {
        getFactory().interrupt(packge, recursive);
    }
    public static void initialize(@NotNull String packge, boolean recursive) throws PluginInitializeException, IOException {
        getFactory().initialize(packge, recursive);
    }

    public static void interrupt(@NotNull ClassLoader loader, @NotNull Package packge, boolean recursive) throws PluginInterruptException {
        getFactory().interrupt(loader, packge, recursive);
    }
    public static void initialize(@NotNull ClassLoader loader, @NotNull Package packge, boolean recursive) throws PluginInitializeException, IOException {
        getFactory().initialize(loader, packge, recursive);
    }

    public static void interrupt(@NotNull Package packge, boolean recursive) throws PluginInterruptException {
        getFactory().interrupt(packge, recursive);
    }
    public static void initialize(@NotNull Package packge, boolean recursive) throws PluginInitializeException, IOException {
        getFactory().initialize(packge, recursive);
    }

    public static void interrupt(@NotNull ClassLoader loader) throws PluginInterruptException {
        getFactory().interrupt(loader);
    }
    @ApiStatus.Experimental
    public static void initialize(@NotNull ClassLoader loader) throws PluginInitializeException, IOException {
        getFactory().initialize(loader);
    }

    @ApiStatus.Experimental
    public static void initializeAll() throws PluginInitializeException, IOException {
        getFactory().initializeAll();
    }
    public static void interruptAll() throws PluginInterruptException {
        getFactory().interruptAll();
    }

    // Object

    private Plugins() {
        throw new UnsupportedOperationException("this class cannot be instantiated");
    }

    // Classes

    private static final class ShutdownHook extends Thread {

        // Object

        public ShutdownHook() {
            super("Plug-ins Shutdown Hook");
        }

        // Modules

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
