package dev.meinicke.plugin.initializer;

import dev.meinicke.plugin.PluginInfo;
import dev.meinicke.plugin.PluginInfo.Builder;
import dev.meinicke.plugin.category.PluginCategory;
import dev.meinicke.plugin.context.PluginContext;
import dev.meinicke.plugin.exception.PluginInitializeException;
import dev.meinicke.plugin.exception.PluginInterruptException;
import dev.meinicke.plugin.main.Plugins;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.io.Flushable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Objects;

/**
 * A concrete implementation of {@link PluginInitializer} that initializes plugin instances using a constructor-based
 * instantiation strategy. This class specifically supports plugin classes that either declare a no-argument constructor
 * or a single-argument constructor accepting a {@link PluginContext}. The constructor with {@code PluginContext} takes
 * precedence if available.
 *
 * <p>Usage requirements:</p>
 * <ul>
 *     <li>The plugin class must declare either a public, protected, package-private, or private constructor that takes
 *     either no parameters or a single {@link PluginContext} parameter.</li>
 *     <li>If both constructors are present, the {@code PluginContext} constructor will be selected for instantiation.</li>
 *     <li>Access restrictions (e.g., private constructors) are lifted via reflection to ensure compatibility.</li>
 * </ul>
 *
 * <p>Initialization procedure:</p>
 * <ol>
 *     <li>The class inspects the plugin reference to locate an appropriate constructor.</li>
 *     <li>The constructor is made accessible and invoked reflectively.</li>
 *     <li>Any exceptions thrown during construction are wrapped and rethrown as {@link PluginInitializeException}, except
 *     in rare unexpected conditions, where a {@link RuntimeException} is used instead.</li>
 *     <li>Upon successful instantiation, the plugin's state is transitioned to RUNNING, and the instance is tracked for
 *     lifecycle management.</li>
 * </ol>
 *
 * <p>Resource cleanup:</p>
 * <ul>
 *     <li>If the plugin instance implements {@link java.io.Closeable}, the {@code close()} method is invoked on shutdown.</li>
 *     <li>If the plugin does not implement {@code Closeable} but implements {@link java.io.Flushable}, the {@code flush()}
 *     method is invoked instead.</li>
 *     <li>If both interfaces are implemented, only {@code close()} is executed as it is assumed to subsume flushing behavior.</li>
 * </ul>
 *
 * <p>All shutdown errors are captured and reported via {@link PluginInterruptException}, and internal plugin references
 * are cleared to free memory.</p>
 */
public final class ConstructorPluginInitializer implements PluginInitializer {

    // Object

    /**
     * Private constructor to prevent instantiation of the initializer class.
     */
    private ConstructorPluginInitializer() {
    }

    // Modules

    /**
     * Creates a new {@link Builder} that encapsulates the plugin's metadata and lifecycle logic.
     * This builder is tied to plugins that support context-aware or no-argument constructor initialization.
     *
     * @param reference    The plugin class. It must declare either a constructor with {@link PluginContext} or a no-arg constructor.
     * @param name         The plugin name. Can be {@code null} if not explicitly defined.
     * @param description  A user-friendly description of the plugin. Can be {@code null}.
     * @param dependencies An array of plugin class references representing required plugin dependencies.
     * @param categories   A list of category tags used to classify this plugin.
     * @param context      The active plugin execution context to be injected during instantiation.
     * @return A {@link Builder} responsible for constructing the plugin's runtime metadata and instance handler.
     */
    @Override
    public @NotNull Builder create(@NotNull Class<?> reference, @Nullable String name, @Nullable String description, @NotNull Class<?> @NotNull [] dependencies, @NotNull String @NotNull [] categories, @NotNull PluginContext context) {
        return new BuilderImpl(reference, name, description, dependencies, categories, context);
    }

    // Implementations

    /**
     * Indicates equality by verifying the other object is also a {@link ConstructorPluginInitializer}.
     *
     * @param obj The object to be compared.
     * @return {@code true} if the object is of the same initializer type.
     */
    @Override
    public boolean equals(@Nullable Object obj) {
        return obj instanceof ConstructorPluginInitializer;
    }
    /**
     * Returns a fixed hash code for this stateless singleton initializer.
     *
     * @return An integer hash code.
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(0);
    }

    // Classes

    private static final class PluginInfoImpl extends PluginInfo {

        public PluginInfoImpl(@NotNull Class<?> reference,
                              @Nullable String name,
                              @Nullable String description,
                              @NotNull PluginInfo @NotNull [] dependencies,
                              @NotNull PluginCategory @NotNull [] categories,
                              @NotNull PluginContext context,
                              int priority) {
            super(reference, name, description, dependencies, categories, ConstructorPluginInitializer.class, priority, context);
        }

        @Override
        public void start() throws PluginInitializeException {
            try {
                // Starting
                setState(State.STARTING);
                handle("start", (handler) -> handler.start(this));
            } catch (@NotNull Throwable throwable) {
                setState(State.FAILED);
                throw new RuntimeException("cannot invoke 'starting' handlers " + getReference().getName(), throwable);
            }

            try {
                // Retrieve constructor
                @NotNull Constructor<?> constructor;

                try {
                    // First try using the constructor with the plugin context parameter
                    constructor = getReference().getDeclaredConstructor(PluginContext.class);
                } catch (@NotNull NoSuchMethodException ignore) {
                    // try now using the blank constructor.
                    constructor = getReference().getDeclaredConstructor();
                }

                // Make it accessible
                constructor.setAccessible(true);

                // Instantiate the plugin using its constructor.
                if (constructor.getParameterCount() == 0) {
                    this.instance = constructor.newInstance();
                } else {
                    this.instance = constructor.newInstance(getContext());
                }
            } catch (@NotNull Throwable throwable) {
                setState(State.FAILED);

                if (throwable instanceof InvocationTargetException) {
                    if (throwable.getCause() instanceof PluginInitializeException) {
                        throw (PluginInitializeException) throwable.getCause();
                    }

                    throw new PluginInitializeException(getReference(), "cannot invoke constructor from class: " + getReference().getName(), throwable.getCause());
                } else if (throwable instanceof NoSuchMethodException) {
                    throw new PluginInitializeException(getReference(), "there's no declared empty constructor at plugin's class: " + getReference().getName(), throwable);
                } else if (throwable instanceof IllegalAccessException) {
                    throw new PluginInitializeException(getReference(), "cannot access declared empty constructor from plugin's class: " + getReference().getName(), throwable);
                } else {
                    throw new RuntimeException("cannot invoke declared empty constructor from plugin: " + getReference().getName(), throwable);
                }
            }

            try {
                // Mark as running
                setState(State.RUNNING);
            } catch (@NotNull Throwable throwable) {
                setState(State.FAILED);
                throw new RuntimeException("cannot invoke 'running' handlers " + getReference().getName(), throwable);
            }
        }
        @Override
        public void close() throws PluginInterruptException {
            if (!getState().isRunning()) {
                return;
            }

            // Super close
            super.close();

            try {
                if (getInstance() instanceof Closeable) {
                    ((Closeable) getInstance()).close();
                } else if (getInstance() instanceof Flushable) {
                    ((Flushable) getInstance()).flush();
                }
            } catch (@NotNull Throwable e) {
                if (e.getCause() instanceof PluginInterruptException) {
                    throw (PluginInterruptException) e.getCause();
                }

                throw new PluginInterruptException(getReference(), "cannot close/flush plugin instance: " + this, e);
            }

            // Finish close
            try {
                handle("close", (handler) -> handler.close(this));
            } finally {
                setState(State.IDLE);
                instance = null;
            }
        }
    }
    private static final class BuilderImpl extends AbstractPluginBuilder {

        // Object

        private BuilderImpl(@NotNull Class<?> reference, @Nullable String name, @Nullable String description, @NotNull Class<?> @NotNull [] dependencies, @NotNull String @NotNull [] categories, @NotNull PluginContext context) {
            super(reference, context);

            // Variables
            name(name);
            description(description);
            dependencies(dependencies);
            categories(categories);
        }

        // Modules

        @Override
        public @NotNull PluginInfo build() {
            @NotNull PluginInfo info = new PluginInfoImpl(getReference(), getName(), getDescription(), dependencies.stream().map(Plugins::retrieve).toArray(PluginInfo[]::new), unregisteredCategories.stream().map(category -> Plugins.getFactory().getCategory(category)).toArray(PluginCategory[]::new), getContext(), getPriority());
            info.getCategories().addAll(registeredCategories);

            return info;
        }

    }

}