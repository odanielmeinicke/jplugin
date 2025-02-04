package codes.laivy.plugin.loader;

import codes.laivy.plugin.info.PluginInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface PluginLoader {
    @NotNull PluginInfo create(@NotNull Class<?> reference, @Nullable String name, @NotNull PluginInfo @NotNull [] dependencies);
}
