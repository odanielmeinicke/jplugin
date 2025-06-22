package dev.meinicke.plugin.main;

import dev.meinicke.plugin.factory.InitializerFactory;
import dev.meinicke.plugin.initializer.PluginInitializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

final class InitializerFactoryImpl implements InitializerFactory {

    // Object

    // Thread safety
    private final @NotNull List<PluginInitializer> initializers = new CopyOnWriteArrayList<>();
    private volatile boolean closed = false;

    public InitializerFactoryImpl() {
    }

    // Getters

    @Override
    public @Unmodifiable @NotNull Collection<@NotNull PluginInitializer> getInitializers() {
        // Verifications
        if (closed) {
            throw new IllegalStateException("this initializer factory is closed.");
        }

        // Finish returning an unmodifiable list of initializers
        return Collections.unmodifiableList(initializers);
    }

    @Override
    public <T extends PluginInitializer> @NotNull T getInitializer(@NotNull Class<T> reference) {
        // Verifications
        if (closed) {
            throw new IllegalStateException("this initializer factory is closed.");
        }

        // Start
        @Nullable PluginInitializer initializer = initializers.stream().filter(i -> i.getClass().equals(reference)).findFirst().orElse(null);

        if (initializer != null) {
            //noinspection unchecked
            return (T) initializer;
        } else {
            try {
                // Constructor
                @NotNull Constructor<T> constructor = reference.getDeclaredConstructor();
                constructor.setAccessible(true);

                return constructor.newInstance();
            } catch (InvocationTargetException e) {
                throw new RuntimeException("cannot execute plugin loader's constructor: " + reference, e);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException("cannot find plugin loader's empty declared constructor: " + reference, e);
            } catch (InstantiationException e) {
                throw new RuntimeException("cannot instantiate plugin loader: " + reference, e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("cannot access plugin loader's constructor: " + reference, e);
            }
        }
    }

    // Modules

    @Override
    public synchronized void close() throws IOException {
        if (closed) return;
        else closed = true;

        // Clear cache
        initializers.clear();
    }

}
