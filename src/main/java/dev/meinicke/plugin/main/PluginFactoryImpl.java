package dev.meinicke.plugin.main;

import dev.meinicke.plugin.Builder;
import dev.meinicke.plugin.PluginInfo;
import dev.meinicke.plugin.annotation.Plugin;
import dev.meinicke.plugin.category.AbstractPluginCategory;
import dev.meinicke.plugin.category.PluginCategory;
import dev.meinicke.plugin.exception.PluginInitializeException;
import dev.meinicke.plugin.exception.PluginInterruptException;
import dev.meinicke.plugin.factory.PluginFactory;
import dev.meinicke.plugin.factory.PluginFinder;
import dev.meinicke.plugin.factory.handlers.Handlers;
import dev.meinicke.plugin.factory.handlers.PluginHandler;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Stream;

final class PluginFactoryImpl implements PluginFactory {

    // Static initializers

    /**
     * This map represents the libraries categories. The key represents the class name the runtime
     * must have to load the category, the value represents the class of the category. The class of the category
     * must have an empty declared constructor.
     */
    private static final @NotNull Map<String, String> libraries = new HashMap<String, String>() {{
        put("com.jlogm.Logger", "dev.meinicke.plugin.category.jlogm.FilterPluginCategory");
    }};

    // Object

    private final @NotNull Map<String, PluginCategory> categories = new HashMap<>();
    private final @NotNull Handlers handlers = Handlers.create();

    final @NotNull Map<Class<?>, PluginInfo> plugins = new LinkedHashMap<>();

    public PluginFactoryImpl() {
        // Default categories
        setCategory(new AutoRegisterPluginCategory());

        // Auto register library categories
        for (@NotNull Entry<String, String> entry : libraries.entrySet()) {
            // Verify if library is present at runtime
            try {
                Class.forName(entry.getKey());
            } catch (@NotNull ClassNotFoundException ignore) {
                continue;
            }

            // Load category
            try {
                @NotNull Class<?> reference = Class.forName(entry.getValue());

                //noinspection unchecked
                @NotNull Constructor<? extends PluginCategory> constructor = (Constructor<? extends PluginCategory>) reference.getDeclaredConstructor();
                constructor.setAccessible(true);

                @NotNull PluginCategory category = constructor.newInstance();
                setCategory(category);
            } catch (@NotNull ClassNotFoundException e) {
                throw new RuntimeException("cannot find category class", e);
            } catch (@NotNull NoSuchMethodException e) {
                throw new RuntimeException("cannot find category constructor", e);
            } catch (@NotNull InvocationTargetException e) {
                throw new RuntimeException("cannot invoke category constructor", e);
            } catch (@NotNull InstantiationException e) {
                throw new RuntimeException("cannot instantiate category", e);
            } catch (@NotNull IllegalAccessException e) {
                throw new RuntimeException("cannot access category constructor", e);
            }
        }
    }

    // Getters

    @Override
    public @NotNull PluginInfo retrieve(@NotNull Class<?> reference) {
        @Nullable PluginInfo info = plugins.getOrDefault(reference, null);

        if (info != null) {
            return info;
        } else if (reference.isAnnotationPresent(Plugin.class)) {
            throw new IllegalArgumentException("the plugin '" + reference.getName() + "' isn't initialized yet");
        } else {
            throw new IllegalArgumentException("the reference '" + reference.getName() + "' isn't a plugin");
        }
    }
    @Override
    public @NotNull PluginInfo retrieve(@NotNull String name) {
        return plugins.values().stream().filter(plugin -> Objects.equals(plugin.getName(), name)).findFirst().orElseThrow(() -> new IllegalArgumentException("there's no plugin with name '" + name + "'"));
    }

    @Override
    public @NotNull Handlers getGlobalHandlers() {
        return handlers;
    }

    // Handlers

    @Override
    public @NotNull PluginCategory getCategory(@NotNull String name) {
        return categories.computeIfAbsent(name.toLowerCase(), k -> new AbstractPluginCategory(name) {});
    }
    @Override
    public @NotNull Optional<PluginCategory> getCategory(@NotNull String name, boolean create) {
        if (create) {
            return Optional.of(categories.computeIfAbsent(name.toLowerCase(), k -> new AbstractPluginCategory(name) {}));
        } else {
            return Optional.ofNullable(categories.getOrDefault(name.toLowerCase(), null));
        }
    }
    @Override
    public boolean hasCategory(@NotNull String name) {
        return categories.containsKey(name.toLowerCase());
    }

    @Override
    public void setCategory(@NotNull PluginCategory category) {
        categories.put(category.getName().toLowerCase(), category);
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
        public boolean accept(@NotNull Builder builder) {
            if (builder.getPriority() == 0) {
                builder.priority(-10);
            }

            return true;
        }

        @Override
        public void run(@NotNull PluginInfo info) {
            @Nullable Object instance = info.getInstance();

            if (!(instance instanceof PluginCategory)) {
                throw new IllegalStateException("the 'Category Reference' plugin doesn't have a PluginCategory instance: " + instance);
            }

            @NotNull PluginCategory category = (PluginCategory) instance;
            categories.put(category.getName().toLowerCase(), category);
        }
        @Override
        public void close(@NotNull PluginInfo info) throws PluginInterruptException {
            @Nullable Object instance = info.getInstance();
            if (!(instance instanceof PluginCategory)) {
                throw new PluginInterruptException(info.getReference(), "the 'Category Reference' plugin doesn't have the PluginCategory instance: " + instance);
            }

            @NotNull PluginCategory category = (PluginCategory) instance;

            category.getPlugins().clear();
            categories.remove(category.getName().toLowerCase());
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
            public boolean accept(@NotNull Builder builder) {
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
