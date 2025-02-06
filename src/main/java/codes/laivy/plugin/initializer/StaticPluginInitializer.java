package codes.laivy.plugin.initializer;

import codes.laivy.plugin.exception.PluginInitializeException;
import codes.laivy.plugin.exception.PluginInterruptException;
import codes.laivy.plugin.info.PluginInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * O StaticPluginInitilaizer não faz nada, não inicializa métodos ou chama construtores, apenas carrega a classe.
 * É útil caso a classe do plugin apenas precise ser carregado para que seu bloco `static {}` seja chamado.
 */
public final class StaticPluginInitializer implements PluginInitializer {

    // Object

    private StaticPluginInitializer() {
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
            } catch (@NotNull Throwable throwable) {
                setState(State.FAILED);
                throw new RuntimeException("cannot initialize plugin: " + getName(), throwable);
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
            } finally {
                instance = null;
                setState(State.IDLE);
            }
        }

    }

}
