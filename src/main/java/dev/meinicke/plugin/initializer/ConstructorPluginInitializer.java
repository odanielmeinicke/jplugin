package dev.meinicke.plugin.initializer;

import dev.meinicke.plugin.Builder;
import dev.meinicke.plugin.PluginInfo;
import dev.meinicke.plugin.category.PluginCategory;
import dev.meinicke.plugin.context.PluginContext;
import dev.meinicke.plugin.exception.InvalidPluginException;
import dev.meinicke.plugin.exception.PluginInitializeException;
import dev.meinicke.plugin.exception.PluginInterruptException;
import dev.meinicke.plugin.factory.PluginFactory;
import dev.meinicke.plugin.main.Plugins;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.io.Flushable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
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

    @Override
    public @NotNull PluginInfo build(@NotNull Builder builder) throws InvalidPluginException {
        // Variables
        @NotNull PluginFactory factory = builder.getFactory();

        // Categories
        @NotNull Collection<PluginCategory> categories = new HashSet<>();
        for (@NotNull String name : builder.getCategories()) {
            categories.add(factory.getCategory(name, true).orElseThrow(() -> new NullPointerException("cannot generate plugin category: " + name)));
        }

        // Dependencies
        @NotNull PluginInfo[] dependencies = Arrays.stream(builder.getDependencies()).map(Plugins::retrieve).toArray(PluginInfo[]::new);

        // Finish
        return new PluginInfoImpl(builder.getReference(), builder.getName(), builder.getDescription(), dependencies, categories.toArray(new PluginCategory[0]), builder.getContext(), builder.getPriority());
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

}