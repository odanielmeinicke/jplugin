package codes.laivy.plugin.main;

import codes.laivy.plugin.exception.PluginInitializeException;
import codes.laivy.plugin.exception.PluginInterruptException;
import codes.laivy.plugin.factory.PluginFactory;
import codes.laivy.plugin.factory.PluginFinder;
import codes.laivy.plugin.factory.handlers.Handlers;
import codes.laivy.plugin.PluginInfo;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

final class PluginFactoryImpl implements PluginFactory {

    // Object

    private final @NotNull Map<String, Handlers> handlers = new HashMap<>();
    final @NotNull Map<Class<?>, PluginInfo> plugins = new LinkedHashMap<>();

    public PluginFactoryImpl() {
        handlers.put(null, Handlers.create());

        // Just to initialize Plugins class (shutdown hook)
        Class<Plugins> reference = Plugins.class;
    }

    // Getters

    @Override
    public @NotNull PluginInfo retrieve(@NotNull Class<?> reference) {
        return plugins.values().stream().filter(plugin -> plugin.getReference().equals(reference)).findFirst().orElseThrow(() -> new IllegalArgumentException("the class '" + reference.getName() + "' isn't a plugin"));
    }
    @Override
    public @NotNull PluginInfo retrieve(@NotNull String name) {
        return plugins.values().stream().filter(plugin -> Objects.equals(plugin.getName(), name)).findFirst().orElseThrow(() -> new IllegalArgumentException("there's no plugin with name '" + name + "'"));
    }

    // Handlers

    @Override
    public @NotNull Handlers getHandlers() {
        return handlers.get(null);
    }
    @Override
    public @NotNull Handlers getHandlers(@NotNull String category) {
        return handlers.computeIfAbsent(category, k -> Handlers.create());
    }

    // Instances

    @Override
    public @NotNull <T> Optional<T> getInstance(@NotNull Class<?> reference) {
        @NotNull PluginInfo plugin = retrieve(reference);

        //noinspection unchecked
        return Optional.ofNullable((T) plugin.getInstance());
    }

    // Initialization and interruption

    @Override
    public void interrupt(@NotNull ClassLoader loader, @NotNull String packge, boolean recursive) throws PluginInterruptException {
        @NotNull List<PluginInfo> plugins = new LinkedList<>(PluginFactoryImpl.this.plugins.values());
        Collections.reverse(plugins);

        for (@NotNull PluginInfo info : plugins) {
            @NotNull String two = info.getReference().getPackage().getName();
            boolean isWithin = recursive ? two.startsWith(packge) : two.equals(packge);

            if (info.getReference().getClassLoader().equals(loader) && !isWithin) {
                continue;
            }

            info.close();
        }
    }
    @Override
    public @NotNull PluginInfo @NotNull [] initialize(@NotNull ClassLoader loader, @NotNull String packge, boolean recursive) throws PluginInitializeException, IOException {
        @NotNull PluginFinder finder = find();
        finder.addClassLoader(loader);
        finder.addPackage(packge, recursive);

        return finder.load();
    }

    @Override
    public void interrupt(@NotNull String packge, boolean recursive) throws PluginInterruptException {
        interrupt(Thread.currentThread().getContextClassLoader(), packge, recursive);
    }
    @Override
    public @NotNull PluginInfo @NotNull [] initialize(@NotNull String packge, boolean recursive) throws PluginInitializeException, IOException {
        @NotNull PluginFinder finder = find();
        finder.addPackage(packge, recursive);

        return finder.load();
    }

    @Override
    public void interrupt(@NotNull ClassLoader loader, @NotNull Package packge, boolean recursive) throws PluginInterruptException {
        interrupt(loader, packge.getName(), recursive);
    }
    @Override
    public @NotNull PluginInfo @NotNull [] initialize(@NotNull ClassLoader loader, @NotNull Package packge, boolean recursive) throws PluginInitializeException, IOException {
        @NotNull PluginFinder finder = find();
        finder.addClassLoader(loader);
        finder.addPackage(packge, recursive);

        return finder.load();
    }

    @Override
    public void interrupt(@NotNull Package packge, boolean recursive) throws PluginInterruptException {
        interrupt(Thread.currentThread().getContextClassLoader(), packge.getName(), recursive);
    }
    @Override
    public @NotNull PluginInfo @NotNull [] initialize(@NotNull Package packge, boolean recursive) throws PluginInitializeException, IOException {
        @NotNull PluginFinder finder = find();
        finder.addPackage(packge, recursive);

        return finder.load();
    }

    @Override
    public void interrupt(@NotNull ClassLoader loader) throws PluginInterruptException {
        @NotNull List<PluginInfo> plugins = new LinkedList<>(PluginFactoryImpl.this.plugins.values());
        Collections.reverse(plugins);

        for (@NotNull PluginInfo info : plugins) {
            @NotNull Class<?> reference = info.getReference();

            if (reference.getClassLoader().equals(loader)) {
                info.close();
            }
        }
    }
    @Override
    @ApiStatus.Experimental
    public @NotNull PluginInfo @NotNull [] initialize(@NotNull ClassLoader loader) throws PluginInitializeException, IOException {
        @NotNull PluginFinder finder = find();
        finder.addClassLoader(loader);

        return finder.load();
    }

    @Override
    @ApiStatus.Experimental
    public @NotNull PluginInfo @NotNull [] initializeAll() throws PluginInitializeException, IOException {
        @NotNull PluginFinder finder = find();
        return finder.load();
    }
    @Override
    public void interruptAll() throws PluginInterruptException {
        @NotNull List<PluginInfo> plugins = new LinkedList<>(PluginFactoryImpl.this.plugins.values());
        Collections.reverse(plugins);

        for (@NotNull PluginInfo info : plugins) {
            info.close();
        }
    }

    // Finders

    @Override
    public @NotNull PluginFinder find() {
        return new PluginFinderImpl(this);
    }

    // Iterator and stream

    @Override
    public @NotNull Iterator<PluginInfo> iterator() {
        return plugins.values().iterator();
    }
    @Override
    public @NotNull Stream<PluginInfo> stream() {
        return plugins.values().stream();
    }

}
