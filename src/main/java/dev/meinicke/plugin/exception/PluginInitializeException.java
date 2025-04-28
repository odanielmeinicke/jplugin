package dev.meinicke.plugin.exception;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class PluginInitializeException extends Exception {

    // Object

    private final @NotNull Class<?> plugin;

    public PluginInitializeException(@NotNull Class<?> plugin, @NotNull String message) {
        super(message);
        this.plugin = plugin;
    }
    public PluginInitializeException(@NotNull Class<?> plugin, @NotNull String message, @Nullable Throwable cause) {
        super(message, cause);
        this.plugin = plugin;
    }
    public PluginInitializeException(@NotNull Class<?> plugin, @Nullable Throwable cause) {
        super(cause);
        this.plugin = plugin;
    }
    public PluginInitializeException(@NotNull Class<?> plugin, @NotNull String message, @Nullable Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.plugin = plugin;
    }

    // Getters

    public @NotNull Class<?> getPlugin() {
        return plugin;
    }

}
