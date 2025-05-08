package dev.meinicke.plugin.initializer;

import dev.meinicke.plugin.PluginInfo;
import dev.meinicke.plugin.attribute.AttributeHolder;
import dev.meinicke.plugin.category.PluginCategory;
import dev.meinicke.plugin.context.PluginContext;
import dev.meinicke.plugin.exception.IllegalAttributeTypeException;
import dev.meinicke.plugin.exception.PluginInitializeException;
import dev.meinicke.plugin.exception.PluginInterruptException;
import dev.meinicke.plugin.main.Plugins;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.io.Flushable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Objects;

/**
 * A plugin initializer that dynamically initializes and optionally tears down plugin components via statically defined methods
 * on the plugin class. This implementation, {@code MethodPluginInitializer}, leverages Java reflection to locate and invoke
 * static methods for the purpose of plugin lifecycle control — specifically, initialization and interruption (shutdown).
 * <p>
 * <b>Initialization Strategy:</b><br>
 * The initializer will attempt to invoke a method (default name: {@code "initialize"}) declared on the plugin class.
 * This method:
 * <ul>
 *   <li><strong>Must be static</strong>: It is not expected to operate on an instance.</li>
 *   <li><strong>May return an object</strong>: If it does, the returned object will be stored as the plugin's runtime instance.</li>
 *   <li><strong>May return void</strong>: In this case, no instance is associated with the plugin.</li>
 *   <li><strong>May optionally accept a {@link PluginContext} as parameter</strong>: If two overloads are present, one with and one without a {@code PluginContext},
 *       the one with {@code PluginContext} takes precedence. This supports plugins that need access to context-specific services or configuration at initialization.</li>
 * </ul>
 * <p>
 * <b>Custom Method Naming:</b><br>
 * Starting with the latest enhancement, plugin developers can now specify alternate names for both the initialization and interruption methods using attributes:
 * <ul>
 *   <li>{@code "initialization method"}: Defines the name of the method to be invoked during plugin startup. If not present, defaults to {@code "initialize"}.</li>
 *   <li>{@code "interruption method"}: Defines the name of the method to be invoked during plugin shutdown. If not present, defaults to {@code "interrupt"}.</li>
 * </ul>
 * These attribute values must be of type {@code String}. Non-string values will result in a runtime {@link IllegalAttributeTypeException}.
 * <p>
 * <b>Cross-Class Method Targeting:</b><br>
 * Those attributes can have a customized class if specified using a {@code '#'} character separating the fully-qualified class name and method name.<br>
 * For example: {@code "com.myapp.plugins.MyPluginInitializerClass#initialize"} will attempt to use the method {@code initialize} from the class {@code com.myapp.plugins.MyPluginInitializerClass}.
 * <ul>
 *   <li>The class will be dynamically resolved using {@code Class.forName(String)} in the same {@link ClassLoader} as the plugin being initialized or interrupted.</li>
 *   <li>The method resolution rules (static, parameter types, return types) remain the same as when targeting the plugin class itself.</li>
 *   <li>If the class or method cannot be found or is invalid, a {@link PluginInitializeException} or {@link PluginInterruptException} will be thrown.</li>
 * </ul>
 * This mechanism allows developers to delegate lifecycle logic to external utility or service classes, promoting modularity and code reuse.
 * <p>
 * <b>Shutdown Strategy:</b><br>
 * During shutdown, {@code MethodPluginInitializer} checks for a static method with the configured interruption name
 * (default {@code "interrupt"}) on the plugin class. This method:
 * <ul>
 *   <li><strong>Must be static</strong>.</li>
 *   <li><strong>Can accept zero or one parameter</strong> — if one parameter is accepted, it will be the instance previously created by the initialization method (if any).</li>
 *   <li>If such a method is found and successfully invoked, no further cleanup is attempted.</li>
 * </ul>
 * If no valid interrupt method is found or an error occurs, the framework attempts an automatic cleanup using interfaces implemented by the plugin instance (if one exists):
 * <ul>
 *   <li>If the instance implements {@link java.io.Closeable}, {@code close()} is invoked.</li>
 *   <li>If the instance does not implement {@code Closeable} but does implement {@link java.io.Flushable}, {@code flush()} is invoked.</li>
 * </ul>
 * Note that these two automatic cleanup methods are mutually exclusive, and {@code close()} takes precedence if both are present.
 * <p>
 * <b>Error Handling:</b><br>
 * This class provides comprehensive error reporting and state management. Any error occurring during startup (e.g., method not found, incorrect signature,
 * reflection access issues, or exceptions thrown during method invocation) results in the plugin being marked as {@link PluginInfo.State#FAILED},
 * and a {@link PluginInitializeException} is thrown.
 * During shutdown, any problem while invoking the interruption method or cleaning up resources results in a {@link PluginInterruptException}.
 * The original cause of the failure is preserved in the thrown exception for maximum debugging fidelity.
 * <p>
 * <b>Thread Safety and Instance Management:</b><br>
 * Plugin instances created via initialization methods are stored internally. However, no synchronization is performed on this storage;
 * it is assumed that lifecycle operations (start, stop) are managed serially by the plugin container.
 * <p>
 * <b>Usage Example:</b><br>
 * Suppose a plugin class is declared as follows:
 * <pre>{@code
 * \@Plugin
 * public final class MyPlugin {
 *     public static MyPlugin initialize(PluginContext context) {
 *         return new MyPlugin();
 *     }
 *     public static void interrupt(MyPlugin instance) {
 *         instance.shutdown();
 *     }
 * }
 * }</pre>
 * If attributes are not explicitly configured, this class will correctly resolve and invoke the {@code initialize} and {@code interrupt} methods by default.
 * <p>
 * Alternatively, if a plugin uses different method names, it can declare them as attributes:
 * <pre>{@code
 * \@Attribute(key = "initialization method", type = String.class, string = "boot")
 * \@Attribute(key = "interruption method", type = String.class, string = "teardown")
 * }</pre>
 * In such cases, the plugin must define:
 * <pre>{@code
 * public static MyPlugin boot(PluginContext context) { ... }
 * public static void teardown(MyPlugin plugin) { ... }
 * }</pre>
 * <p>
 * You may also point these attributes to methods declared in separate classes:
 * <pre>{@code
 * \@Attribute(key = "initialization method", type = String.class, string = "com.myapp.plugins.InitHelper#boot")
 * \@Attribute(key = "interruption method", type = String.class, string = "com.myapp.plugins.InitHelper#teardown")
 * }</pre>
 * <p>
 * <b>Design Notes:</b><br>
 * This class is final and cannot be subclassed. It is stateless and thread-safe by design, implemented as a singleton-style utility with
 * a private constructor and equality semantics overridden to ensure consistency across plugin graph traversal.
 *
 * @see PluginInitializer
 * @see PluginContext
 * @see PluginInfo
 * @see PluginInitializeException
 * @see PluginInterruptException
 * @see AttributeHolder
 */
public final class MethodPluginInitializer implements PluginInitializer {

    // Object

    /**
     * Private constructor to prevent instantiation.
     */
    private MethodPluginInitializer() {
    }

    // Modules

    /**
     * Creates a {@link PluginInfo} instance for the provided plugin class by instantiating an internal
     * {@link PluginInfoImpl} that encapsulates the plugin's metadata and lifecycle management logic.
     *
     * @param reference    The plugin class annotated with {@code @Plugin}.
     * @param name         The name of the plugin, which may be null if not explicitly specified.
     * @param description  A textual description of the plugin's functionality, which may be null.
     * @param dependencies An array of {@link PluginInfo} objects representing the dependencies required by the plugin.
     * @param categories   An array of category tags that classify the plugin.
     * @param context      The context associated with the plugin.
     * @return A fully constructed {@link PluginInfo} instance that manages the initialization and shutdown
     *         of the plugin.
     */
    @Override
    public @NotNull PluginInfo.Builder create(@NotNull Class<?> reference, @Nullable String name, @Nullable String description, @NotNull Class<?> @NotNull [] dependencies, @NotNull String @NotNull [] categories, @NotNull PluginContext context) {
        return new BuilderImpl(reference, name, description, dependencies, categories, context);
    }

    // Implementations

    @Override
    public boolean equals(@Nullable Object obj) {
        return obj instanceof MethodPluginInitializer;
    }
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
            super(reference, name, description, dependencies, categories, MethodPluginInitializer.class, priority, context);
        }

        @Override
        public void start() throws PluginInitializeException {
            // Get method name by attributes
            @NotNull String methodName = "initialize";
            @NotNull Class<?> methodClass = getReference();

            {
                @Nullable AttributeHolder nameAttribute = getContext().getAttributes().findByKey("initialization method").orElse(null);

                if (nameAttribute != null) {
                    if (nameAttribute.isString()) {
                        @NotNull String string = nameAttribute.getAsString();

                        if (string.contains("#")) {
                            @NotNull String[] parts = string.split("#", 2);

                            if (parts.length == 2) {
                                try {
                                    methodClass = Class.forName(parts[0]);
                                } catch (@NotNull ClassNotFoundException e) {
                                    throw new IllegalStateException("invalid class for initialization method attribute", e);
                                }

                                methodName = parts[1];
                            } else {
                                methodName = parts[0];
                            }
                        } else {
                            methodName = nameAttribute.getAsString();
                        }
                    } else {
                        throw new IllegalAttributeTypeException("the initialization method attribute must be a string: " + nameAttribute.getValue().getClass().getName());
                    }
                }
            }

            // Start initialization
            try {
                // Starting
                setState(State.STARTING);
                handle("start", (handler) -> handler.start(this));

                // Find the initialization method
                @NotNull Method method;

                try {
                    // First try to retrieve method with the context parameter
                    method = methodClass.getDeclaredMethod(methodName, PluginContext.class);
                } catch (@NotNull NoSuchMethodException ignore) {
                    // Not found, try to retrieve method without the plugin context parameter
                    method = methodClass.getDeclaredMethod(methodName);
                }

                // Make it accessible
                method.setAccessible(true);

                // Verify that the initialize method is static; if not, throw an exception.
                if (!Modifier.isStatic(method.getModifiers())) {
                    throw new IllegalStateException("the plugin's initialize method must be static");
                }

                // Invoke the static initialization method. It may return an instance or be void.
                if (method.getParameterCount() == 0) {
                    this.instance = method.invoke(null);
                } else {
                    this.instance = method.invoke(null, getContext());
                }

                // Mark as running
                setState(State.RUNNING);
            } catch (@NotNull Throwable throwable) {
                setState(State.FAILED);
                if (throwable instanceof InvocationTargetException) {
                    if (throwable.getCause() instanceof PluginInitializeException) {
                        throw (PluginInitializeException) throwable.getCause();
                    }
                    throw new PluginInitializeException(getReference(), "cannot invoke initialization method: " + methodName, throwable.getCause());
                } else if (throwable instanceof NoSuchMethodException) {
                    throw new PluginInitializeException(getReference(), "cannot find initialization method: " + methodName, throwable);
                } else if (throwable instanceof IllegalAccessException) {
                    throw new PluginInitializeException(getReference(), "cannot access initialization method: " + methodName, throwable);
                } else {
                    throw new RuntimeException("cannot initialize plugin: " + this, throwable);
                }
            }
        }

        @Override
        public void close() throws PluginInterruptException {
            if (!getState().isRunning()) {
                return;
            }

            // Super close
            super.close();

            // Get method name by attributes
            @NotNull String methodName = "interrupt";
            @NotNull Class<?> methodClass = getReference();

            {
                @Nullable AttributeHolder nameAttribute = getContext().getAttributes().findByKey("interruption method").orElse(null);

                if (nameAttribute != null) {
                    if (nameAttribute.isString()) {
                        @NotNull String string = nameAttribute.getAsString();

                        if (string.contains("#")) {
                            @NotNull String[] parts = string.split("#", 2);

                            if (parts.length == 2) {
                                try {
                                    methodClass = Class.forName(parts[0]);
                                } catch (@NotNull ClassNotFoundException e) {
                                    throw new IllegalStateException("invalid class for interruption method attribute", e);
                                }

                                methodName = parts[1];
                            } else {
                                methodName = nameAttribute.getAsString();
                            }
                        } else {
                            methodName = nameAttribute.getAsString();
                        }
                    } else {
                        throw new IllegalAttributeTypeException("the interruption method attribute must be a string: " + nameAttribute.getValue().getClass().getName());
                    }
                }
            }

            try {
                try {
                    @Nullable Method method = null;
                    for (@NotNull Method target : methodClass.getDeclaredMethods()) {
                        if (!target.getName().equals(methodName)) {
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
                        } catch (@NotNull Throwable e) {
                            throw new PluginInterruptException(getReference(), "cannot close/flush plugin instance: " + this, e);
                        }
                    }
                } catch (@NotNull InvocationTargetException e) {
                    if (e.getCause() instanceof PluginInterruptException) {
                        throw (PluginInterruptException) e.getCause();
                    }

                    // Try to close instance
                    try {
                        if (getInstance() instanceof Closeable) {
                            ((Closeable) getInstance()).close();
                        } else if (getInstance() instanceof Flushable) {
                            ((Flushable) getInstance()).flush();
                        }
                    } catch (@NotNull Throwable ignore) {
                        // Does nothing...
                    }

                    // Throw interrupt exception
                    throw new PluginInterruptException(getReference(), "cannot invoke interruption method: " + methodName, e);
                } catch (@NotNull IllegalAccessException e) {
                    throw new PluginInterruptException(getReference(), "cannot access interruption method: " + methodName, e);
                }

                // Finish close
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
            super(reference, context, name, description, dependencies, categories);
        }

        // Modules

        @Override
        public @NotNull PluginInfo build() {
            @NotNull PluginInfo info = new PluginInfoImpl(getReference(), getName(), getDescription(), dependencies.stream().map(Plugins::retrieve).toArray(PluginInfo[]::new), unregisteredCategories.stream().map(category -> Plugins.getPluginFactory().getCategory(category)).toArray(PluginCategory[]::new), getContext(), getPriority());
            info.getCategories().addAll(registeredCategories);

            return info;
        }

    }

}