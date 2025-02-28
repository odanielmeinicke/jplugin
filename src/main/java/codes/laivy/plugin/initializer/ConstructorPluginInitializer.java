package codes.laivy.plugin.initializer;

import codes.laivy.plugin.PluginInfo;
import codes.laivy.plugin.category.PluginCategory;
import codes.laivy.plugin.exception.PluginInitializeException;
import codes.laivy.plugin.exception.PluginInterruptException;
import codes.laivy.plugin.main.Plugins;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.io.Flushable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * A concrete implementation of {@link PluginInitializer} that initializes a plugin by invoking its no-argument
 * constructor. This initializer is strictly designed for plugin classes that explicitly declare a no-argument constructor,
 * regardless of its visibility (public, protected, package-private, or private). It is imperative that plugins using this
 * initializer adhere to this requirement; failure to do so will result in a runtime exception during the initialization phase.
 * <p>
 * Upon initialization, the no-argument constructor of the plugin class is invoked using reflection. The resulting
 * instance is then stored and managed throughout the plugin's lifecycle. If the plugin class implements {@link java.io.Closeable}
 * or {@link java.io.Flushable}, this instance will be automatically subjected to proper resource cleanup during the shutdown
 * phase. In cases where the instance implements both interfaces, only the {@code close()} method is executed, giving precedence
 * to the more comprehensive cleanup semantics typically associated with {@code Closeable}.
 * <p>
 * The implementation guarantees that:
 * <ul>
 *   <li>The plugin class is inspected to locate a declared no-argument constructor. This constructor is made accessible
 *       regardless of its declared visibility.</li>
 *   <li>Any exception during the invocation of the constructor is captured, and detailed error information is provided by
 *       throwing a {@link PluginInitializeException} (or a {@link RuntimeException} in unexpected cases) after marking the
 *       plugin's state as FAILED.</li>
 *   <li>After a successful instantiation, the plugin's state is set to RUNNING, indicating that the plugin is fully
 *       initialized and operational.</li>
 * </ul>
 * <p>
 * During the shutdown process, the {@link Closeable#close()} method is invoked to perform resource cleanup:
 * <ul>
 *   <li>If the plugin instance implements {@link java.io.Closeable}, the {@code close()} method of the instance is called.</li>
 *   <li>If the instance does not implement {@code Closeable} but implements {@link java.io.Flushable}, then the {@code flush()} method is invoked.</li>
 *   <li>If the instance implements both interfaces, only the {@code close()} method is executed.</li>
 * </ul>
 * <p>
 * Detailed error handling during shutdown ensures that any issues encountered during resource cleanup result in a
 * {@link PluginInterruptException} being thrown with comprehensive diagnostic information. The internal plugin instance
 * is cleared and the state is set to IDLE regardless of the outcome, ensuring the plugin is fully terminated.
 */
public final class ConstructorPluginInitializer implements PluginInitializer {

    /**
     * Private constructor to prevent instantiation of this initializer.
     */
    private ConstructorPluginInitializer() {
    }

    /**
     * Creates a {@link PluginInfo} instance for the given plugin class by instantiating an internal
     * {@link PluginInfoImpl} that encapsulates the plugin's metadata and lifecycle management logic.
     *
     * @param reference    The plugin class annotated with {@code @Plugin}. It must declare a no-argument constructor.
     * @param name         The plugin's name; may be null if not explicitly specified.
     * @param description  A textual description of the plugin's functionality; may be null.
     * @param dependencies An array of {@link PluginInfo} objects representing the dependencies required by the plugin.
     * @param categories   An array of category tags that classify the plugin.
     * @return A fully constructed {@link PluginInfo} instance managing the initialization and shutdown lifecycle of the plugin.
     */
    @Override
    public @NotNull PluginInfo.Builder create(@NotNull Class<?> reference,
                                      @Nullable String name,
                                      @Nullable String description,
                                      @NotNull Class<?> @NotNull [] dependencies,
                                      @NotNull String @NotNull [] categories) {
        return new BuilderImpl(reference, name, description, dependencies, categories);
    }

    /**
     * Internal implementation of {@link PluginInfo} for plugins that utilize a no-argument constructor for initialization.
     * <p>
     * This class encapsulates the lifecycle management logic for the plugin, including startup and shutdown operations.
     * The startup process involves:
     * <ol>
     *   <li>Invoking the superclass {@code start()} method to perform any preliminary tasks.</li>
     *   <li>Using reflection to obtain the declared no-argument constructor of the plugin class. This constructor is set
     *       as accessible to bypass any visibility restrictions.</li>
     *   <li>Invoking the constructor to create a new instance of the plugin.</li>
     *   <li>If the constructor invocation is successful, the resulting instance is stored and the plugin state is updated to RUNNING.</li>
     * </ol>
     * <p>
     * In the event of any exception during the instantiation process, the following measures are taken:
     * <ul>
     *   <li>The plugin state is set to FAILED.</li>
     *   <li>If the exception is an {@link InvocationTargetException} and its cause is a {@link PluginInitializeException},
     *       the underlying cause is rethrown.</li>
     *   <li>If the exception is a {@link NoSuchMethodException}, a {@link PluginInitializeException} is thrown with a message
     *       indicating that no declared empty constructor is available.</li>
     *   <li>If the exception is an {@link IllegalAccessException}, a {@link PluginInitializeException} is thrown indicating
     *       that the empty constructor is inaccessible.</li>
     *   <li>Any other exception results in a {@link RuntimeException} being thrown, providing detailed context about the failure.</li>
     * </ul>
     * <p>
     * The shutdown process, handled by the {@link #close()} method, performs the following steps:
     * <ol>
     *   <li>Invokes the superclass {@code close()} to execute any preliminary shutdown routines.</li>
     *   <li>If the plugin instance implements {@link java.io.Closeable}, its {@code close()} method is invoked to release
     *       any held resources.</li>
     *   <li>If the instance does not implement {@code Closeable} but implements {@link java.io.Flushable}, then its {@code flush()} method is invoked.</li>
     *   <li>In cases where the instance implements both interfaces, only the {@code close()} method is executed.</li>
     *   <li>Any exception during these cleanup operations is caught and rethrown as a {@link PluginInterruptException} with detailed error information.</li>
     *   <li>Finally, the plugin instance is cleared (set to null) and the plugin state is transitioned to IDLE, marking the completion
     *       of the shutdown process.</li>
     * </ol>
     */
    private static final class PluginInfoImpl extends PluginInfo {

        /**
         * Constructs a new {@link PluginInfoImpl} instance with the specified plugin metadata and associates it with
         * the {@link ConstructorPluginInitializer}.
         *
         * @param reference    The plugin class reference that must contain a no-argument constructor.
         * @param name         The name of the plugin; may be null.
         * @param description  A description of the plugin's functionality; may be null.
         * @param dependencies An array of {@link PluginInfo} objects representing the plugin's dependencies.
         * @param categories   An array of category tags used to classify the plugin.
         * @param priority     The priority of this plugin
         */
        public PluginInfoImpl(@NotNull Class<?> reference,
                              @Nullable String name,
                              @Nullable String description,
                              @NotNull PluginInfo @NotNull [] dependencies,
                              @NotNull PluginCategory @NotNull [] categories,
                              int priority) {
            super(reference, name, description, dependencies, categories, ConstructorPluginInitializer.class, priority);
        }

        /**
         * Starts the plugin by invoking its declared no-argument constructor.
         * <p>
         * The method performs the following sequence of operations:
         * <ol>
         *   <li>Calls {@code super.start()} to execute any necessary preliminary startup logic.</li>
         *   <li>Uses reflection to obtain the no-argument constructor from the plugin class, ensuring it is accessible even
         *       if declared private or with other restricted visibility.</li>
         *   <li>Invokes the constructor to create a new instance of the plugin.</li>
         *   <li>If the instance is successfully created, it is stored internally and the plugin state is updated to RUNNING.</li>
         * </ol>
         * <p>
         * If any exception occurs during the instantiation process, the following error handling is applied:
         * <ul>
         *   <li>An {@link InvocationTargetException} is caught, and if its underlying cause is a {@link PluginInitializeException},
         *       that exception is rethrown; otherwise, a new {@link PluginInitializeException} is thrown with a detailed message
         *       including the plugin class name and the underlying cause.</li>
         *   <li>A {@link NoSuchMethodException} indicates that the plugin class does not declare an empty constructor, and
         *       a corresponding {@link PluginInitializeException} is thrown.</li>
         *   <li>An {@link IllegalAccessException} is handled similarly by throwing a {@link PluginInitializeException} with a message
         *       indicating the constructor's inaccessibility.</li>
         *   <li>Any other {@code Throwable} leads to a {@link RuntimeException} being thrown, indicating a critical failure
         *       during plugin initialization.</li>
         * </ul>
         *
         * @throws PluginInitializeException If the no-argument constructor cannot be found, accessed, or successfully invoked,
         *                                   or if any error occurs during the instantiation of the plugin.
         */
        @Override
        public void start() throws PluginInitializeException {
            try {
                // Starting
                setState(State.STARTING);
                handle("start", (handler) -> handler.start(this));

                // Generate instance
                @NotNull Constructor<?> constructor = getReference().getDeclaredConstructor();
                constructor.setAccessible(true);
                // Instantiate the plugin using its no-argument constructor.
                this.instance = constructor.newInstance();

                // Mark as running
                setState(State.RUNNING);
            } catch (@NotNull Throwable throwable) {
                setState(State.FAILED);
                if (throwable instanceof InvocationTargetException) {
                    if (throwable.getCause() instanceof PluginInitializeException) {
                        throw (PluginInitializeException) throwable.getCause();
                    }
                    throw new PluginInitializeException(getReference(), "cannot invoke constructor from class: " +
                            getReference().getName(), throwable.getCause());
                } else if (throwable instanceof NoSuchMethodException) {
                    throw new PluginInitializeException(getReference(), "there's no declared empty constructor at plugin's class: " +
                            getReference().getName(), throwable);
                } else if (throwable instanceof IllegalAccessException) {
                    throw new PluginInitializeException(getReference(), "cannot access declared empty constructor from plugin's class: " +
                            getReference().getName(), throwable);
                } else {
                    throw new RuntimeException("cannot invoke declared empty constructor from plugin: " +
                            getReference().getName(), throwable);
                }
            }
        }

        /**
         * Shuts down the plugin by performing resource cleanup on the plugin instance.
         * <p>
         * During shutdown, the following operations are performed:
         * <ol>
         *   <li>Invokes {@code super.close()} to execute any preliminary shutdown routines.</li>
         *   <li>If the plugin instance exists and implements {@link java.io.Closeable}, its {@code close()} method is called.
         *       Otherwise, if the instance implements {@link java.io.Flushable}, its {@code flush()} method is invoked.
         *       In cases where the instance implements both, only the {@code close()} method is executed.</li>
         *   <li>If any exception occurs during these operations, it is caught and rethrown as a {@link PluginInterruptException}
         *       with detailed contextual information.</li>
         *   <li>Finally, the plugin instance is cleared (set to null) and the plugin state is updated to IDLE, indicating
         *       that the plugin has been fully shut down.</li>
         * </ol>
         * <p>
         * It is important to note that the shutdown process is executed only if the plugin is in a RUNNING state.
         *
         * @throws PluginInterruptException If any error occurs during the invocation of resource cleanup methods on the plugin instance.
         */
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

        private BuilderImpl(@NotNull Class<?> reference, @Nullable String name, @Nullable String description, @NotNull Class<?> @NotNull [] dependencies, @NotNull String @NotNull [] categories) {
            super(reference);

            // Variables
            name(name);
            description(description);
            dependencies(dependencies);
            categories(categories);
        }

        // Modules

        @Override
        public @NotNull PluginInfo build() {
            @NotNull PluginInfo info = new PluginInfoImpl(getReference(), getName(), getDescription(), dependencies.stream().map(Plugins::retrieve).toArray(PluginInfo[]::new), unregisteredCategories.stream().map(category -> Plugins.getFactory().getCategory(category)).toArray(PluginCategory[]::new), getPriority());
            info.getCategories().addAll(registeredCategories);

            return info;
        }

    }

}