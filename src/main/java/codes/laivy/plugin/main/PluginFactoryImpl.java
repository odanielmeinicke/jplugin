package codes.laivy.plugin.main;

import codes.laivy.plugin.PluginInfo;
import codes.laivy.plugin.category.AbstractPluginCategory;
import codes.laivy.plugin.category.PluginCategory;
import codes.laivy.plugin.exception.PluginInitializeException;
import codes.laivy.plugin.exception.PluginInterruptException;
import codes.laivy.plugin.factory.PluginFactory;
import codes.laivy.plugin.factory.PluginFinder;
import codes.laivy.plugin.factory.handlers.PluginHandler;
import codes.laivy.plugin.initializer.ConstructorPluginInitializer;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

final class PluginFactoryImpl implements PluginFactory {

    // Object

    private final @NotNull Map<String, PluginCategory> categories = new HashMap<>();
    final @NotNull Map<Class<?>, PluginInfo> plugins = new LinkedHashMap<>();

    public PluginFactoryImpl() {
        // Just to initialize Plugins class (shutdown hook)
        Class<Plugins> reference = Plugins.class;

        // Default categories
        setCategory(new AutoRegisterPluginCategory());
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
    public @NotNull PluginCategory getCategory(@NotNull String name) {
        return categories.computeIfAbsent(name, k -> new AbstractPluginCategory(name) {});
    }
    @Override
    public void setCategory(@NotNull PluginCategory category) {
        categories.put(category.getName(), category);
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

    // Classes

    private final class AutoRegisterPluginCategory extends AbstractPluginCategory {

        // Object

        private AutoRegisterPluginCategory() {
            super("Category Reference");
        }

        // Modules

        @Override
        public boolean accept(PluginInfo.@NotNull Builder builder) {
            if (PluginCategory.class.isAssignableFrom(builder.getReference())) {
                builder.initializer(ConstructorPluginInitializer.class);
                builder.getHandlers().add(new HandlerImpl(builder.getReference()));

                return true;
            }

            return false;
        }
        @Override
        public boolean accept(@NotNull PluginInfo info) {
            if (info.getInstance() instanceof PluginCategory) {
                info.getHandlers().add(new HandlerImpl(info.getReference()));

                return true;
            }

            return false;
        }

        @Override
        public void start(@NotNull PluginInfo info) throws PluginInitializeException {
            if (!(info.getInstance() instanceof PluginCategory)) {
                throw new PluginInitializeException(info.getReference(), "the 'Category Reference' plugin doesn't have a plugin category instance: " + info.getInstance());
            }

            @NotNull PluginCategory category = (PluginCategory) info.getInstance();
            categories.put(category.getName(), category);
        }
        @Override
        public void close(@NotNull PluginInfo info) throws PluginInterruptException {
            if (!(info.getInstance() instanceof PluginCategory)) {
                throw new PluginInterruptException(info.getReference(), "the 'Category Reference' plugin doesn't have the plugin category instance: " + info.getInstance());
            }

            @NotNull PluginCategory category = (PluginCategory) info.getInstance();

            category.getPlugins().clear();
            categories.remove(category.getName());
        }

        // Classes

        private final class HandlerImpl implements PluginHandler {

            // Object

            private final @NotNull Class<?> category;

            private HandlerImpl(@NotNull Class<?> category) {
                this.category = category;
            }

            // Modules

            @Override
            public boolean accept(PluginInfo.@NotNull Builder builder) {
                builder.dependency(category);
                return PluginHandler.super.accept(builder);
            }
            @Override
            public boolean accept(@NotNull PluginInfo info) {
                info.getDependencies().remove(Plugins.retrieve(category));
                return PluginHandler.super.accept(info);
            }

        }

    }

}
