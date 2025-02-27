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
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * A concrete implementation of {@link PluginInitializer} that initializes a plugin by invoking a static
 * {@code initialize} method on the plugin class. This implementation is designed for plugins that use
 * a static method for their initialization logic, which can optionally return an instance of the plugin.
 * <p>
 * The static {@code initialize} method must be declared in the plugin class and is invoked without
 * any parameters. It can either return an object instance or have a {@code void} return type. If the method
 * returns an instance, that object is stored and considered the plugin's instance for later use in the
 * plugin lifecycle; if it returns {@code void}, then no instance is stored and the plugin will only
 * undergo static initialization.
 * <p>
 * The stored instance, if present, is subject to lifecycle management. In a typical shutdown procedure,
 * if no custom interrupt method is provided, the instance is examined: if it implements {@link java.io.Closeable}
 * or {@link java.io.Flushable}, the corresponding {@code close()} or {@code flush()} method is invoked (with
 * {@code close()} taking precedence if both are implemented). However, if an {@code interrupt} method is
 * found, the resource cleanup through {@code Closeable} or {@code Flushable} is bypassed.
 * <p>
 * This implementation also searches for an optional static {@code interrupt} method in the plugin class.
 * The {@code interrupt} method may have either zero parameters or a single, nullable parameter that represents
 * the plugin instance. If the method accepts a parameter and the plugin instance exists, the instance is passed
 * to the method; otherwise, the method is invoked without parameters. If the {@code interrupt} method is present,
 * it takes precedence over any automatic instance resource cleanup, meaning that if it is executed successfully,
 * the instance's {@code close()} or {@code flush()} methods will not be called.
 * <p>
 * Additionally, if multiple static {@code initialize} methods exist (one accepting a plugin instance parameter and
 * one without), the first method encountered (in declaration order) will be executed.
 * <p>
 * Detailed error handling is integrated into both the startup and shutdown procedures. During initialization,
 * any issues such as the absence of the {@code initialize} method, failure in method access, or exceptions thrown
 * during invocation (including those wrapped in an {@link InvocationTargetException}) will result in the plugin's
 * state being set to FAILED and a corresponding {@link PluginInitializeException} being thrown. Similarly, any
 * problems encountered while invoking the {@code interrupt} method or performing resource cleanup during shutdown
 * will lead to a {@link PluginInterruptException} with detailed context.
 */
// TODO: 13/02/2025 Add method names
public final class MethodPluginInitializer implements PluginInitializer {

    /**
     * Private constructor to prevent instantiation.
     */
    private MethodPluginInitializer() {
    }

    /**
     * Creates a {@link PluginInfo} instance for the provided plugin class by instantiating an internal
     * {@link PluginInfoImpl} that encapsulates the plugin's metadata and lifecycle management logic.
     *
     * @param reference    The plugin class annotated with {@code @Plugin}.
     * @param name         The name of the plugin, which may be null if not explicitly specified.
     * @param description  A textual description of the plugin's functionality, which may be null.
     * @param dependencies An array of {@link PluginInfo} objects representing the dependencies required by the plugin.
     * @param categories   An array of category tags that classify the plugin.
     * @return A fully constructed {@link PluginInfo} instance that manages the initialization and shutdown
     *         of the plugin.
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
     * Internal implementation of {@link PluginInfo} for plugins that use a static method-based initialization.
     * <p>
     * This class encapsulates the complete lifecycle management logic of the plugin. During startup,
     * it attempts to locate and invoke a static {@code initialize} method on the plugin class. The method
     * is required to be static; if it is not, an {@link IllegalStateException} is thrown. Once invoked, the
     * method may either return an object instance—which is then stored as the plugin's instance—or have a
     * {@code void} return type, in which case no instance is stored.
     * <p>
     * During shutdown, the {@code close()} method first searches for an optional static {@code interrupt} method.
     * The search is performed over all declared methods of the plugin class, and only methods named {@code interrupt}
     * that are static and have at most one parameter are considered. If such a method is found:
     * <ul>
     *     <li>If the {@code interrupt} method accepts one parameter, the current plugin instance (which may be null)
     *         is passed as an argument.</li>
     *     <li>If the {@code interrupt} method accepts no parameters, it is invoked without any arguments.</li>
     * </ul>
     * If the {@code interrupt} method is executed, any automatic resource cleanup (i.e. invoking {@code close()} on
     * a {@link java.io.Closeable} instance or {@code flush()} on a {@link java.io.Flushable} instance) is skipped.
     * <p>
     * If no valid {@code interrupt} method is found and an instance exists, the implementation then checks if the
     * instance implements {@link java.io.Closeable} or {@link java.io.Flushable}. If it implements {@code Closeable},
     * its {@code close()} method is invoked; if not, but it implements {@code Flushable}, then its {@code flush()} method
     * is called. In cases where the instance implements both, only {@code close()} is executed.
     * <p>
     * Both the startup and shutdown procedures incorporate comprehensive error handling. Any failure during
     * the invocation of the {@code initialize} method will result in the plugin's state being marked as FAILED,
     * and an appropriate {@link PluginInitializeException} will be thrown. Similarly, any issues during the
     * shutdown phase—whether in invoking the {@code interrupt} method or during resource cleanup—will cause a
     * {@link PluginInterruptException} to be thrown with detailed contextual information.
     */
    private static final class PluginInfoImpl extends PluginInfo {

        /**
         * Constructs a new {@link PluginInfoImpl} instance with the specified metadata and associates it with
         * the {@link MethodPluginInitializer}.
         *
         * @param reference    The plugin class reference.
         * @param name         The name of the plugin, which may be null.
         * @param description  A description of the plugin, which may be null.
         * @param dependencies An array of {@link PluginInfo} objects representing required dependencies.
         * @param categories   An array of category tags for the plugin.
         */
        public PluginInfoImpl(@NotNull Class<?> reference,
                              @Nullable String name,
                              @Nullable String description,
                              @NotNull PluginInfo @NotNull [] dependencies,
                              @NotNull PluginCategory @NotNull [] categories) {
            super(reference, name, description, dependencies, categories, MethodPluginInitializer.class);
        }

        /**
         * Starts the plugin by locating and invoking its static {@code initialize} method.
         * <p>
         * The method performs the following steps:
         * <ol>
         *     <li>Invokes the superclass {@code start()} to perform any preliminary startup tasks.</li>
         *     <li>Searches for a method named {@code initialize} declared in the plugin class.
         *         The method must be static; if it is not, an {@link IllegalStateException} is thrown.
         *         If multiple {@code initialize} methods exist (one accepting a plugin instance parameter and one without),
         *         the first encountered method is executed.</li>
         *     <li>Sets the method as accessible and invokes it without parameters.</li>
         *     <li>If the {@code initialize} method returns an object instance, this instance is stored for later use;
         *         if it returns {@code void}, no instance is stored and the plugin is treated as having no instance.</li>
         *     <li>Upon successful execution, the plugin state is set to RUNNING.</li>
         * </ol>
         * <p>
         * Any exceptions occurring during these steps (including issues with method lookup, access, or invocation)
         * are caught and handled as follows:
         * <ul>
         *     <li>If an {@link InvocationTargetException} is encountered and its cause is an instance of
         *         {@link PluginInitializeException}, the cause is rethrown.</li>
         *     <li>Otherwise, a new {@link PluginInitializeException} is thrown with a detailed message and the underlying cause.</li>
         *     <li>The plugin state is set to FAILED if any error occurs.</li>
         * </ul>
         *
         * @throws PluginInitializeException If the {@code initialize} method cannot be found, accessed, or invoked properly,
         *                                   or if it results in an exception during plugin startup.
         */
        @Override
        public void start() throws PluginInitializeException {
            try {
                // Starting
                setState(State.STARTING);
                handle("start", (handler) -> handler.start(this));

                // Initialize by method
                @NotNull Method method = getReference().getDeclaredMethod("initialize");
                method.setAccessible(true);
                // Verify that the initialize method is static; if not, throw an exception.
                if (!Modifier.isStatic(method.getModifiers())) {
                    throw new IllegalStateException("the plugin's initialize method must be static");
                }
                // Invoke the static initialize method. It may return an instance or be void.
                this.instance = method.invoke(null);

                // Mark as running
                setState(State.RUNNING);
            } catch (@NotNull Throwable throwable) {
                setState(State.FAILED);
                if (throwable instanceof InvocationTargetException) {
                    if (throwable.getCause() instanceof PluginInitializeException) {
                        throw (PluginInitializeException) throwable.getCause();
                    }
                    throw new PluginInitializeException(getReference(), "cannot invoke initialize method", throwable.getCause());
                } else if (throwable instanceof NoSuchMethodException) {
                    throw new PluginInitializeException(getReference(), "cannot find initialize method", throwable);
                } else if (throwable instanceof IllegalAccessException) {
                    throw new PluginInitializeException(getReference(), "cannot access initialize method", throwable);
                } else {
                    throw new RuntimeException("cannot initialize plugin: " + this, throwable);
                }
            }
        }

        /**
         * Shuts down the plugin by first attempting to invoke an optional static {@code interrupt} method,
         * and then, if necessary, performing resource cleanup on the plugin instance.
         * <p>
         * The shutdown procedure proceeds as follows:
         * <ol>
         *     <li>Invokes the superclass {@code close()} to perform any preliminary shutdown tasks.</li>
         *     <li>Iterates over all declared methods of the plugin class to search for a static method named
         *         {@code interrupt} that has at most one parameter. If multiple candidate methods exist, the first
         *         one encountered is selected.</li>
         *     <li>If an {@code interrupt} method is found:
         *         <ul>
         *             <li>If the method accepts one parameter, it is invoked with the current plugin instance
         *                 (which may be null) as the argument.</li>
         *             <li>If the method accepts no parameters, it is invoked without any arguments.</li>
         *         </ul>
         *         When the {@code interrupt} method is invoked successfully, any automatic resource cleanup of the
         *         plugin instance (i.e. invoking {@code close()} on a {@link java.io.Closeable} or {@code flush()} on
         *         a {@link java.io.Flushable}) is bypassed.</li>
         *     <li>If no suitable {@code interrupt} method is found and a plugin instance exists, the method then
         *         checks whether the instance implements {@link java.io.Closeable} or {@link java.io.Flushable}:
         *         <ul>
         *             <li>If the instance implements {@code Closeable}, its {@code close()} method is invoked.</li>
         *             <li>If not, but the instance implements {@code Flushable}, its {@code flush()} method is invoked.</li>
         *             <li>If the instance implements both interfaces, only the {@code close()} method is executed.</li>
         *         </ul>
         *     </li>
         *     <li>Any exceptions during the invocation of the {@code interrupt} method or during resource cleanup are
         *         caught and rethrown as a {@link PluginInterruptException} with detailed error information.</li>
         *     <li>Finally, regardless of any errors, the internal plugin instance is cleared (set to null) and the
         *         plugin state is set to IDLE, indicating a complete shutdown.</li>
         * </ol>
         *
         * @throws PluginInterruptException If an error occurs during the invocation of the {@code interrupt} method,
         *                                  or if there is an issue during the resource cleanup of the plugin instance.
         */
        @Override
        public void close() throws PluginInterruptException {
            if (!getState().isRunning()) {
                return;
            }

            // Super close
            super.close();

            try {
                @Nullable Method method = null;
                for (@NotNull Method target : getReference().getDeclaredMethods()) {
                    if (!target.getName().equals("interrupt")) {
                        continue;
                    } else if (!Modifier.isStatic(target.getModifiers())) {
                        continue;
                    } else if (target.getParameterCount() > 1) {
                        continue;
                    }

                    method = target;
                    break;
                }

                // If an interrupt method is found, invoke it. The method may accept one parameter (the plugin instance)
                // or no parameters. If invoked, resource cleanup via Closeable/Flushable is bypassed.
                if (method != null) {
                    method.setAccessible(true);
                    if (method.getParameterCount() == 1) {
                        method.invoke(null, getInstance());
                    } else {
                        method.invoke(null);
                    }
                } else if (getInstance() != null) {
                    try {
                        if (getInstance() instanceof Closeable) {
                            ((Closeable) getInstance()).close();
                        } else if (getInstance() instanceof Flushable) {
                            ((Flushable) getInstance()).flush();
                        }
                    } catch (@NotNull IOException e) {
                        throw new PluginInterruptException(getReference(), "cannot close/flush plugin instance: " + this, e);
                    }
                }
            } catch (@NotNull InvocationTargetException e) {
                if (e.getCause() instanceof PluginInterruptException) {
                    throw (PluginInterruptException) e.getCause();
                }

                throw new PluginInterruptException(getReference(), "cannot invoke interrupt method", e);
            } catch (@NotNull IllegalAccessException e) {
                throw new PluginInterruptException(getReference(), "cannot access interrupt method", e);
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
            @NotNull PluginInfo info = new PluginInfoImpl(getReference(), getName(), getDescription(), dependencies.stream().map(Plugins::retrieve).toArray(PluginInfo[]::new), unregisteredCategories.stream().map(category -> Plugins.getFactory().getCategory(category)).toArray(PluginCategory[]::new));
            info.getCategories().addAll(registeredCategories);

            return info;
        }

    }

}