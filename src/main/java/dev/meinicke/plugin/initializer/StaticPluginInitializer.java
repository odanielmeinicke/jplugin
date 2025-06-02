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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

/**
 * A minimal {@link PluginInitializer} implementation that performs no special lifecycle logic
 * and does not create or manage a plugin instance.
 * <p>
 * {@code StaticPluginInitializer} simply ensures that the plugin class is loaded into the JVM
 * during plugin initialization. It performs a no-op for interruption (shutdown) and does not
 * invoke any static methods such as {@code initialize()} or {@code interrupt()}.
 * <p>
 * <b>Initialization Behavior:</b><br>
 * On initialization, this class:
 * <ul>
 *   <li>Attempts to load the plugin class using {@link Class#forName(String)} in the plugin's {@link ClassLoader}.</li>
 *   <li>If the class is already loaded, no further action is taken.</li>
 *   <li>No plugin instance is created or stored.</li>
 *   <li>No reflection or method invocation is performed.</li>
 * </ul>
 * This is useful for plugins that rely entirely on static initialization blocks or class-level constructs,
 * and do not require explicit lifecycle handling.
 * <p>
 * ⚠️ <b>Note on {@code static { }} blocks:</b><br>
 * While the plugin class is guaranteed to be loaded, <strong>static initializers are not guaranteed to run at plugin loading time</strong>
 * if the JVM or class loader already resolved the class earlier for any reason. Thus, relying on {@code static {}} blocks to perform
 * plugin registration or critical initialization is discouraged unless the plugin class is never referenced before initialization.
 * <p>
 * <b>Interruption Behavior:</b><br>
 * This class performs no action during plugin shutdown. It assumes the plugin has no cleanup logic or handles it independently.
 * <p>
 * <b>Thread Safety:</b><br>
 * This implementation is stateless and fully thread-safe.
 * <p>
 * <b>Usage:</b><br>
 * Use {@code StaticPluginInitializer} when you want the plugin system to only recognize and load a plugin class, but not perform
 * any active initialization or instance tracking.
 * <pre>{@code
 * \@Initializer(StaticPluginInitializer.class)
 * \@Plugin
 * public final class MyPassivePlugin {
 *     static {
 *         // This block may or may not be called depending on class load timing
 *     }
 * }
 * }</pre>
 * <p>
 * This is ideal for utility-only plugins, annotation processors, service auto-registrars, or plugin bridges that do not require full lifecycle hooks.
 *
 * @see PluginInitializer
 * @see MethodPluginInitializer
 */
public final class StaticPluginInitializer implements PluginInitializer {

    // Object

    private StaticPluginInitializer() {
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

        // Object

        public PluginInfoImpl(@NotNull Class<?> reference, @Nullable String name, @Nullable String description, @NotNull PluginInfo @NotNull [] dependencies, @NotNull PluginCategory @NotNull [] categories, @NotNull PluginContext context, int priority) {
            super(reference, name, description, dependencies, categories, StaticPluginInitializer.class, priority, context);
        }

        @Override
        public void start() throws PluginInitializeException {
            // Starting
            setState(State.STARTING);
            handle("start", (handler) -> handler.start(this));

            // Mark as running
            setState(State.RUNNING);
        }
        @Override
        public void close() throws PluginInterruptException {
            // Super close
            super.close();

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
