package dev.meinicke.plugin.main;

import dev.meinicke.plugin.PluginInfo;
import dev.meinicke.plugin.exception.PluginInterruptException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

final class ShutdownHookThread extends Thread {

    // Static initializers

    private static final @NotNull Logger log = LoggerFactory.getLogger(ShutdownHookThread.class);
    private static final @NotNull AtomicInteger THREAD_COUNT = new AtomicInteger(0);

    // Object

    private final @NotNull Collection<PluginInfo> plugins;

    public ShutdownHookThread(@NotNull Collection<PluginInfo> plugins) {
        super("Plug-ins Shutdown Hook #" + THREAD_COUNT.getAndIncrement());
        this.plugins = plugins;
    }

    // Getters

    private @NotNull Collection<PluginInfo> getPlugins() {
        return plugins;
    }

    // Modules

    @Override
    public void run() {
        @NotNull List<PluginInfo> plugins = new LinkedList<>(getPlugins());
        Collections.reverse(plugins);

        // Loop
        for (@NotNull PluginInfo info : plugins) {
            if (!info.isAutoClose()) {
                continue;
            } else if (info.getState() != PluginInfo.State.RUNNING) {
                continue;
            }

            try {
                info.close();
            } catch (@NotNull PluginInterruptException e) {
                @NotNull String name = info.getName() != null ? info.getName() : info.getReference().getName();
                log.error("Cannot gracefully unload plugin \"{}\": {}", name, e.getMessage(), e);
            }
        }
    }
}
