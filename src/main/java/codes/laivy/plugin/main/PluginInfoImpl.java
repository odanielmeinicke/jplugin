package codes.laivy.plugin.main;

import codes.laivy.plugin.PluginInfo;
import codes.laivy.plugin.exception.PluginInitializeException;
import codes.laivy.plugin.exception.PluginInterruptException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

final class PluginInfoImpl implements PluginInfo {

    // Object

    private final @NotNull Object lock = new Object();

    private final @Nullable String name;
    private final @NotNull Class<?> reference;

    private final @NotNull Class<?> @NotNull [] dependencies;
    public final @NotNull Set<@NotNull PluginInfoImpl> dependants = new LinkedHashSet<>();

    private volatile @NotNull State state = State.IDLE;

    public PluginInfoImpl(@Nullable String name, @NotNull Class<?> reference, @NotNull Class<?> @NotNull [] dependencies) {
        this.name = name;
        this.reference = reference;
        this.dependencies = dependencies;

        // Register this plugin
        Plugins.plugins.put(reference, this);
    }

    // Getters

    public @NotNull String getName() {
        return name != null ? name : getReference().getName();
    }

    public @NotNull Class<?> getReference() {
        return reference;
    }

    @Override
    @Unmodifiable
    public @NotNull Collection<@NotNull PluginInfo> getDependencies() {
        synchronized (lock) {
            return Arrays.stream(dependencies).map(Plugins::retrieve).collect(Collectors.toSet());
        }
    }
    @Override
    @Unmodifiable
    public @NotNull Collection<@NotNull PluginInfo> getDependants() {
        synchronized (lock) {
            return Collections.unmodifiableSet(dependants);
        }
    }

    public @NotNull State getState() {
        return state;
    }

    public boolean isRunning() {
        return getState() == State.RUNNING;
    }

    // Implementations

    @Override
    public boolean equals(@Nullable Object object) {
        if (this == object) return true;
        if (!(object instanceof PluginInfoImpl)) return false;
        @NotNull PluginInfoImpl that = (PluginInfoImpl) object;
        return Objects.equals(getReference(), that.getReference());
    }
    @Override
    public int hashCode() {
        return Objects.hashCode(getReference());
    }

    @Override
    public @NotNull String toString() {
        return getName();
    }

    // Modules

    @Override
    public void start() throws PluginInitializeException {
        state = State.STARTING;

        try {
            @NotNull Method method = getReference().getDeclaredMethod("initialize");
            method.setAccessible(true);

            // Invoke method
            method.invoke(null);
        } catch (@NotNull Throwable throwable) {
            state = State.FAILED;

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
                throw new RuntimeException("cannot initialize plugin: " + getName(), throwable);
            }
        }

        // Mark as running
        state = State.RUNNING;
    }
    @Override
    public void close() throws PluginInterruptException {
        if (!isRunning()) {
            return;
        }

        // Dependencies
        @NotNull PluginInfo[] dependants = getDependants().stream().filter(dependency -> dependency.getState() != State.IDLE && dependency.getState() != State.FAILED).toArray(PluginInfo[]::new);

        if (dependants.length == 0) {
            @NotNull String list = Arrays.toString(dependants);
            list = list.substring(1, list.length() - 1);

            throw new PluginInterruptException(reference, "cannot interrupt plugin '" + getName() + "' because there's active dependants: " + list);
        }

        // Mark as stopping
        state = State.STOPPING;

        try {
            @NotNull Method method = getReference().getDeclaredMethod("interrupt");
            method.setAccessible(true);

            // Invoke method
            method.invoke(null);
        } catch (@NotNull InvocationTargetException e) {
            if (e.getCause() instanceof PluginInterruptException) {
                throw (PluginInterruptException) e.getCause();
            }

            throw new PluginInterruptException(getReference(), "cannot invoke interrupt method", e.getCause());
        } catch (@NotNull NoSuchMethodException e) {
            throw new PluginInterruptException(getReference(), "cannot find interrupt method", e);
        } catch (@NotNull IllegalAccessException e) {
            throw new PluginInterruptException(getReference(), "cannot access interrupt method", e);
        } finally {
            state = State.IDLE;
        }
    }

}
