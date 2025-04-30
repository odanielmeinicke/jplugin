package dev.meinicke.plugin.main;

import dev.meinicke.plugin.PluginInfo;
import dev.meinicke.plugin.annotation.Attribute;
import dev.meinicke.plugin.attribute.Attributes;
import dev.meinicke.plugin.context.PluginContext;
import dev.meinicke.plugin.exception.IllegalAttributeTypeException;
import dev.meinicke.plugin.factory.PluginFinder;
import dev.meinicke.plugin.metadata.Metadata;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;

final class PluginContextImpl implements PluginContext {

    private final @NotNull Class<?> pluginClass;

    @Nullable PluginInfo plugin;
    final @NotNull Set<PluginInfo> plugins = new LinkedHashSet<>();

    private final @NotNull Metadata metadata = new Metadata();
    private final @NotNull Attributes attributes = new Attributes();

    private final @NotNull Class<?> caller;
    private final @Nullable PluginFinder finder;

    public PluginContextImpl(@NotNull Class<?> pluginClass, @NotNull Class<?> caller, @Nullable PluginFinder finder) {
        this.pluginClass = pluginClass;
        this.caller = caller;
        this.finder = finder;

        // Retrieve attributes
        for (@NotNull Attribute attribute : pluginClass.getAnnotationsByType(Attribute.class)) {
            @NotNull String key = attribute.key();
            @NotNull Class<?> type = attribute.type();
            @NotNull Object object;

            if (type == String.class) {
                getAttributes().put(key, attribute.string());
            } else if (type == String[].class) {
                getAttributes().put(key, attribute.stringArray());
            } else if (type == Class.class) {
                getAttributes().put(key, attribute.classValue());
            } else if (type == Class[].class) {
                getAttributes().put(key, attribute.classArray());
            } else if (type == int.class || type == Integer.class) {
                getAttributes().put(key, attribute.integer());
            } else if (type == long.class || type == Long.class) {
                getAttributes().put(key, attribute.longValue());
            } else if (type == double.class || type == Double.class) {
                getAttributes().put(key, attribute.doubleValue());
            } else if (type == float.class || type == Float.class) {
                getAttributes().put(key, attribute.floatValue());
            } else if (type == boolean.class || type == Boolean.class) {
                getAttributes().put(key, attribute.booleanValue());
            } else if (type == short.class || type == Short.class) {
                getAttributes().put(key, attribute.shortValue());
            } else if (type == char.class || type == Character.class) {
                getAttributes().put(key, attribute.character());
            } else {
                throw new IllegalAttributeTypeException("illegal attribute type: " + type);
            }
        }
    }

    // Getters

    @Override
    public @NotNull Class<?> getPluginClass() {
        return pluginClass;
    }

    @Override
    public @NotNull PluginInfo getCurrentPlugin() {
        if (plugin == null) {
            throw new IllegalStateException("it's not possible to retrieve the plugin now because the builder hasn't been build yet.");
        }

        return plugin;
    }
    @Override
    public @Unmodifiable @NotNull Collection<PluginInfo> getAllPlugins() {
        if (plugin == null) {
            throw new IllegalStateException("it's not possible to retrieve the plugin now because the builder hasn't been build yet.");
        }

        return Collections.unmodifiableSet(plugins);
    }

    @Override
    public @NotNull Metadata getMetadata() {
        return metadata;
    }
    @Override
    public @NotNull Attributes getAttributes() {
        return attributes;
    }

    @Override
    public @NotNull Class<?> getCallerClass() {
        return caller;
    }
    @Override
    public @Nullable PluginFinder getFinder() {
        return finder;
    }

    // Implementations

    @Override
    public boolean equals(@Nullable Object object) {
        if (this == object) return true;
        if (!(object instanceof PluginContextImpl)) return false;
        @NotNull PluginContextImpl that = (PluginContextImpl) object;
        return Objects.equals(getPluginClass(), that.getPluginClass());
    }
    @Override
    public int hashCode() {
        return Objects.hashCode(getPluginClass());
    }

    @Override
    public @NotNull String toString() {
        return "PluginContextImpl{" +
                "class=" + pluginClass +
                ", plugin=" + plugin +
                ", plugins=" + plugins +
                ", metadata=" + metadata +
                ", attributes=" + attributes +
                ", caller=" + caller +
                ", finder=" + finder +
                '}';
    }

}
