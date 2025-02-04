package codes.laivy.plugin.initializer;

import codes.laivy.plugin.exception.PluginInitializeException;
import codes.laivy.plugin.exception.PluginInterruptException;
import codes.laivy.plugin.info.PluginInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.io.Flushable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public final class ConstructorPluginInitializer implements PluginInitializer {

    // Object

    private ConstructorPluginInitializer() {
    }

    // Modules

    @Override
    public @NotNull PluginInfo create(@NotNull Class<?> reference, @Nullable String name, @Nullable String description, @NotNull PluginInfo @NotNull [] dependencies) {
        return new PluginInfoImpl(reference, name, description, dependencies);
    }

    // Classes

    private static final class PluginInfoImpl extends PluginInfo {

        // Object

        public PluginInfoImpl(@NotNull Class<?> reference, @Nullable String name, @Nullable String description, @NotNull PluginInfo @NotNull [] dependencies) {
            super(reference, name, description, dependencies);
        }

        // Modules

        @Override
        public void start() throws PluginInitializeException {
            super.start();

            try {
                @NotNull Constructor<?> constructor = getReference().getDeclaredConstructor();
                constructor.setAccessible(true);

                // Invoke method
                this.instance = constructor.newInstance();
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

            // Mark as running
            setState(State.RUNNING);
        }
        @Override
        public void close() throws PluginInterruptException {
            if (!getState().isRunning()) {
                return;
            }

            try {
                super.close();

                if (getInstance() instanceof Closeable) {
                    ((Closeable) getInstance()).close();
                } else if (getInstance() instanceof Flushable) {
                    ((Flushable) getInstance()).flush();
                }
            } catch (@NotNull Throwable e) {
                if (e.getCause() instanceof PluginInterruptException) {
                    throw (PluginInterruptException) e.getCause();
                }

                throw new PluginInterruptException(getReference(), "cannot invoke interrupt method", e.getCause());
            } finally {
                instance = null;
                setState(State.IDLE);
            }
        }

    }

}