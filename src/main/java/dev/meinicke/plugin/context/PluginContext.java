package dev.meinicke.plugin.context;

import dev.meinicke.plugin.PluginInfo;
import dev.meinicke.plugin.factory.PluginFinder;
import dev.meinicke.plugin.PluginInfo;
import dev.meinicke.plugin.factory.PluginFinder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface PluginContext {

    @NotNull
    PluginInfo.Builder getSelfBuilder();

    @NotNull Metadata getMetadata();

    @NotNull Class<?> getCallerClass();
    @Nullable Object getCallerInstance();

    @Nullable
    PluginFinder getFinder();
    @NotNull PluginInfo.Builder getTotalBuilders();
}
