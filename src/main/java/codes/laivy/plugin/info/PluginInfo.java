package codes.laivy.plugin.info;

import codes.laivy.plugin.annotation.Category;
import codes.laivy.plugin.annotation.Initializer;
import codes.laivy.plugin.category.PluginHandler;
import codes.laivy.plugin.exception.PluginInitializeException;
import codes.laivy.plugin.exception.PluginInterruptException;
import codes.laivy.plugin.factory.handlers.Handlers;
import codes.laivy.plugin.loader.ConstructorPluginLoader;
import codes.laivy.plugin.main.Plugins;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;

public abstract class PluginInfo {

    // Object

    private final @Nullable String name;
    private final @NotNull Class<?> reference;

    private final @NotNull PluginInfo @NotNull [] dependencies;
    public final @NotNull Set<@NotNull PluginInfo> dependants = new LinkedHashSet<>();

    private volatile @NotNull State state = State.IDLE;
    protected @Nullable Object instance;

    private final @NotNull Handlers handlers = Handlers.create();

    public PluginInfo(@NotNull Class<?> reference, @Nullable String name, @NotNull PluginInfo @NotNull [] dependencies) {
        this.reference = reference;
        this.name = name;
        this.dependencies = dependencies;
    }

    // Getters

    public @NotNull String getName() {
        return name != null ? name : getReference().getName();
    }

    public final @NotNull State getState() {
        return state;
    }
    protected void setState(@NotNull State state) {
        @NotNull State previous = this.state;
        this.state = state;

        handle("change state", (handler) -> handler.state(this, previous));

        if (state == State.RUNNING) {
            handle("mark as running", (handler) -> handler.run(this));
        }
    }

    public final @NotNull Class<?> getReference() {
        return reference;
    }

    @Unmodifiable
    public @NotNull Collection<@NotNull PluginInfo> getDependencies() {
        return Arrays.asList(dependencies);
    }
    public final @NotNull Collection<@NotNull PluginInfo> getDependants() {
        return dependants;
    }

    public @NotNull Handlers getHandlers() {
        return handlers;
    }

    /**
     * Represents the instance of this plugin, the instance of the reference. It could be
     * null if the {@link Initializer} uses a PluginLoader that generates the instance like {@link ConstructorPluginLoader}
     *
     * @return
     */
    public final @Nullable Object getInstance() {
        return instance;
    }

    // Modules

    public void start() throws PluginInitializeException {
        setState(State.STARTING);
        handle("start", (handler) -> handler.start(this));
    }
    public void close() throws PluginInterruptException {
        if (getState().isRunning()) {
            return;
        }

        // Dependencies
        @NotNull PluginInfo[] dependants = getDependants().stream().filter(dependency -> dependency.getState() != State.IDLE && dependency.getState() != State.FAILED).toArray(PluginInfo[]::new);

        if (dependants.length > 0) {
            @NotNull String list = Arrays.toString(dependants);
            list = list.substring(1, list.length() - 1);

            throw new PluginInterruptException(reference, "cannot interrupt plugin '" + getName() + "' because there's active dependants: " + list);
        }

        // Mark as stopping
        setState(State.STOPPING);
        handle("close", (handler) -> handler.close(this));

        // The implementation should do the rest
    }

    // Implementations

    @Override
    public final boolean equals(@Nullable Object object) {
        if (this == object) return true;
        if (!(object instanceof PluginInfo)) return false;
        @NotNull PluginInfo that = (PluginInfo) object;
        return Objects.equals(getReference(), that.getReference());
    }
    @Override
    public final int hashCode() {
        return Objects.hashCode(getReference());
    }

    @Override
    public final @NotNull String toString() {
        return getName();
    }

    // Classes

    public enum State {

        IDLE,
        FAILED,

        STARTING,
        RUNNING,
        STOPPING,
        ;

        public boolean isRunning() {
            return this == RUNNING;
        }
        public boolean isIdle() {
            return this == IDLE || this == FAILED;
        }

    }

    // Utilities

    private void handle(@NotNull String action, @NotNull ThrowingConsumer<PluginHandler> consumer) {
        // Call handlers
        {
            // Plugin handlers
            for (@NotNull PluginHandler handler : getHandlers()) {
                try {
                    consumer.accept(handler);
                } catch (@NotNull Throwable throwable) {
                    throw new RuntimeException("cannot invoke plugin's handler to " + action + " '" + getName() + "': " + handler);
                }
            }

            // Category handlers
            for (@NotNull Category category : getReference().getAnnotationsByType(Category.class)) {
                @NotNull String name = category.name();

                for (@NotNull PluginHandler handler : Plugins.getFactory().getHandlers(name)) {
                    try {
                        consumer.accept(handler);
                    } catch (@NotNull Throwable throwable) {
                        throw new RuntimeException("cannot invoke category's handler to " + action + " '" + getName() + "': " + handler);
                    }
                }
            }

            // Global handlers
            for (@NotNull PluginHandler handler : Plugins.getFactory().getHandlers()) {
                try {
                    consumer.accept(handler);
                } catch (@NotNull Throwable throwable) {
                    throw new RuntimeException("cannot invoke global handler to " + action + " '" + getName() + "': " + handler);
                }
            }
        }
    }
    @FunctionalInterface
    public interface ThrowingConsumer<T> {
        void accept(T t) throws Throwable;
    }

}
