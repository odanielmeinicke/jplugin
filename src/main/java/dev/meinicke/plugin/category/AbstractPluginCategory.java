package dev.meinicke.plugin.category;

import dev.meinicke.plugin.PluginInfo;
import dev.meinicke.plugin.factory.handlers.Handlers;
import dev.meinicke.plugin.main.Plugins;
import dev.meinicke.plugin.PluginInfo;
import dev.meinicke.plugin.factory.handlers.Handlers;
import dev.meinicke.plugin.main.Plugins;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.io.IOException;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.stream.Collectors;

public abstract class AbstractPluginCategory implements PluginCategory, Closeable {

    // Object

    private final @NotNull String name;
    private final @NotNull Handlers handlers = Handlers.create();
    private final @NotNull Collection<@NotNull PluginInfo> plugins = new CollectionImpl();

    public AbstractPluginCategory(@NotNull String name) {
        this.name = name;
    }

    // Getters

    @Override
    public final @NotNull String getName() {
        return name;
    }
    @Override
    public final @NotNull Handlers getHandlers() {
        return handlers;
    }
    @Override
    public final @NotNull Collection<@NotNull PluginInfo> getPlugins() {
        return plugins;
    }

    // Modules

    @Override
    public void close() throws IOException {
    }

    // Implementations

    @Override
    public final boolean equals(@Nullable Object object) {
        if (!(object instanceof PluginCategory)) return false;
        @NotNull PluginCategory that = (PluginCategory) object;
        return getName().equalsIgnoreCase(that.getName());
    }
    @Override
    public final int hashCode() {
        return Objects.hash(getName().toLowerCase());
    }

    @Override
    public final @NotNull String toString() {
        return getName();
    }

    // Classes

    private final class CollectionImpl extends AbstractSet<PluginInfo> {

        @Override
        public boolean add(@NotNull PluginInfo info) {
            return info.getCategories().add(AbstractPluginCategory.this);
        }
        @Override
        public boolean remove(@NotNull Object o) {
            if (o instanceof PluginInfo) {
                @NotNull PluginInfo info = (PluginInfo) o;
                return info.getCategories().remove(AbstractPluginCategory.this);
            } else {
                return false;
            }
        }

        // Modules

        private @NotNull Collection<PluginInfo> collect() {
            return Plugins.getFactory().stream().filter(plugin -> plugin.getCategories().contains(AbstractPluginCategory.this)).collect(Collectors.toSet());
        }

        // Iterator and size

        @Override
        public @NotNull Iterator<@NotNull PluginInfo> iterator() {
            return collect().iterator();
        }
        @Override
        public int size() {
            return collect().size();
        }

    }

}
