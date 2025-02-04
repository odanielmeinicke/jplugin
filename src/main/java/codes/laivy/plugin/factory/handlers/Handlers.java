package codes.laivy.plugin.factory.handlers;

import codes.laivy.plugin.category.PluginHandler;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.stream.Stream;

public interface Handlers extends Iterable<PluginHandler> {

    // Static initializers

    static @NotNull Handlers create() {
        return new HandlersImpl();
    }

    // Object

    boolean add(@NotNull PluginHandler handler);
    void add(int index, @NotNull PluginHandler handler);

    void addFirst(@NotNull PluginHandler handler);
    void addLast(@NotNull PluginHandler handler);

    boolean remove(@NotNull PluginHandler handler);

    void clear();
    int size();

    default boolean isEmpty() {
        return size() == 0;
    }

    // Modules

    @Override
    @NotNull Iterator<PluginHandler> iterator();
    @NotNull Stream<PluginHandler> stream();

    // Classes

}
