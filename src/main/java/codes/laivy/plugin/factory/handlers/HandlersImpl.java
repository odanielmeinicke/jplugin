package codes.laivy.plugin.factory.handlers;

import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.stream.Stream;

final class HandlersImpl implements Handlers {

    // Object

    private final @NotNull LinkedList<PluginHandler> list = new LinkedList<>();

    public HandlersImpl() {
    }

    // Modules

    @Override
    public boolean add(@NotNull PluginHandler handler) {
        return list.add(handler);
    }

    @Override
    public void add(int index, @NotNull PluginHandler handler) {
        list.add(index, handler);
    }

    @Override
    public void addFirst(@NotNull PluginHandler handler) {
        list.addLast(handler);
    }

    @Override
    public void addLast(@NotNull PluginHandler handler) {
        list.addLast(handler);
    }

    @Override
    public boolean remove(@NotNull PluginHandler handler) {
        return list.remove(handler);
    }

    @Override
    public void clear() {
        list.clear();
    }

    // Getters

    @Override
    public int size() {
        return list.size();
    }

    // Iterator and stream

    @Override
    public @NotNull Iterator<PluginHandler> iterator() {
        return list.iterator();
    }

    @Override
    public @NotNull Stream<PluginHandler> stream() {
        return list.stream();
    }

}
