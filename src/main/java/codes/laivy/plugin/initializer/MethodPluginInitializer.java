package codes.laivy.plugin.initializer;

import codes.laivy.plugin.exception.PluginInitializeException;
import codes.laivy.plugin.exception.PluginInterruptException;
import codes.laivy.plugin.info.PluginInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * O MethodPluginInitializer requer que a classe do plugin haja dois métodos estáticos:
 * 1. Um method de inicialização chamado
 */
public final class MethodPluginInitializer implements PluginInitializer {

    // Object

    private MethodPluginInitializer() {
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
            try {
                super.start();

                @NotNull Method method = getReference().getDeclaredMethod("initialize");
                method.setAccessible(true);

                // Invoke method
                this.instance = method.invoke(null);
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
                    throw new RuntimeException("cannot initialize plugin: " + getName(), throwable);
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

                @NotNull Method method = getReference().getDeclaredMethod("interrupt");
                method.setAccessible(true);

                // Invoke method
                method.invoke(null);

                // Close instance
                if (getInstance() != null) try {
                    if (getInstance() instanceof Closeable) {
                        ((Closeable) getInstance()).close();
                    } else if (getInstance() instanceof Flushable) {
                        ((Flushable) getInstance()).flush();
                    }
                } catch (@NotNull IOException e) {
                    throw new PluginInterruptException(getReference(), "cannot close/flush plugin instance: " + getName());
                }
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
                instance = null;
                setState(State.IDLE);
            }
        }

    }

}
