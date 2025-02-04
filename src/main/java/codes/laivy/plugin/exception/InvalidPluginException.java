package codes.laivy.plugin.exception;

import org.jetbrains.annotations.NotNull;

public final class InvalidPluginException extends RuntimeException {

    // Object

    private final @NotNull Class<?> plugin;

    public InvalidPluginException(@NotNull Class<?> plugin, @NotNull String message) {
        super(message);
        this.plugin = plugin;
    }
    public InvalidPluginException(@NotNull Class<?> plugin, @NotNull String message, @NotNull Throwable cause) {
        super(message, cause);
        this.plugin = plugin;
    }
    public InvalidPluginException(@NotNull Class<?> plugin, @NotNull Throwable cause) {
        super(cause);
        this.plugin = plugin;
    }
    public InvalidPluginException(@NotNull Class<?> plugin, @NotNull String message, @NotNull Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.plugin = plugin;
    }

    // Getters

    public @NotNull Class<?> getPlugin() {
        return plugin;
    }

}
