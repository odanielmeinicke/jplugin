package codes.laivy.plugin.factory;

import codes.laivy.plugin.exception.PluginInitializeException;
import codes.laivy.plugin.info.PluginInfo;
import codes.laivy.plugin.initializer.PluginInitializer;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.function.Predicate;

public interface PluginFinder {

    @Contract(value = "_->this")
    @NotNull PluginFinder classLoaders(@NotNull ClassLoader @NotNull ... loaders);

    @Contract(value = "_->this")
    @NotNull PluginFinder addClassLoader(@NotNull ClassLoader loader);

    @Contract(value = "_->this")
    @NotNull PluginFinder categories(@NotNull String @NotNull ... categories);

    @Contract(value = "_->this")
    @NotNull PluginFinder addCategory(@NotNull String category);

    @Contract(value = "_->this")
    @NotNull PluginFinder packages(@NotNull Package @NotNull ... packages);

    @Contract(value = "_->this")
    @NotNull PluginFinder packages(@NotNull String @NotNull ... packages);

    @Contract(value = "_->this")
    @NotNull PluginFinder addPackage(@NotNull String packge);

    @Contract(value = "_,_->this")
    @NotNull PluginFinder addPackage(@NotNull String packge, boolean recursive);

    @Contract(value = "_->this")
    @NotNull PluginFinder addPackage(@NotNull Package packge);

    @Contract(value = "_,_->this")
    @NotNull PluginFinder addPackage(@NotNull Package packge, boolean recursive);

    @Contract(value = "_->this")
    @NotNull PluginFinder initializers(@NotNull Class<? extends PluginInitializer> @NotNull [] initializers);

    @Contract(value = "_->this")
    @NotNull PluginFinder initializer(@NotNull Class<? extends PluginInitializer> initializer);

    @Contract(value = "_->this")
    @NotNull PluginFinder addInitializer(@NotNull Class<? extends PluginInitializer> initializer);

    @Contract(value = "_->this")
    @NotNull PluginFinder names(@NotNull String @NotNull ... names);

    @Contract(value = "_->this")
    @NotNull PluginFinder addName(@NotNull String name);

    @Contract(value = "_->this")
    @NotNull PluginFinder descriptions(@NotNull String @NotNull ... descriptions);

    @Contract(value = "_->this")
    @NotNull PluginFinder addDescription(@NotNull String description);

    @Contract(value = "_->this")
    @NotNull PluginFinder dependencies(@NotNull Class<?> @NotNull ... dependencies);
    @Contract(value = "_->this")
    @NotNull PluginFinder dependencies(@NotNull PluginInfo @NotNull ... dependencies);
    @Contract(value = "_->this")
    @NotNull PluginFinder addDependency(@NotNull Class<?> dependency);
    @Contract(value = "_->this")
    @NotNull PluginFinder addDependency(@NotNull PluginInfo dependency);

    @Contract(value = "_->this")
    @NotNull PluginFinder dependants(@NotNull Class<?> @NotNull ... dependants);
    @Contract(value = "_->this")
    @NotNull PluginFinder dependants(@NotNull PluginInfo @NotNull ... dependants);
    @Contract(value = "_->this")
    @NotNull PluginFinder addDependant(@NotNull Class<?> dependant);
    @Contract(value = "_->this")
    @NotNull PluginFinder addDependant(@NotNull PluginInfo dependant);

    @Contract(value = "_->this")
    @NotNull PluginFinder instances(@NotNull Object @NotNull ... instances);

    @Contract(value = "_->this")
    @NotNull PluginFinder addInstance(@NotNull Object instance);

    @Contract(value = "_->this")
    @NotNull PluginFinder states(@NotNull PluginInfo.State @NotNull ... states);
    @Contract(value = "_->this")
    @NotNull PluginFinder addState(@NotNull PluginInfo.State state);

    /**
     * Checks if a plugin info matches with that plugin finder parameters
     *
     * @param info
     * @return
     */
    boolean matches(@NotNull PluginInfo info);
    boolean matches(@NotNull Class<?> reference);

    /**
     * Retrieve the plugins with those configurations
     *
     * @return
     */
    @NotNull PluginInfo @NotNull [] plugins();

    /**
     * Retrieve the classes with those configurations, this action will look into
     * all classes (including unloaded), and will analyze if they're compatible.
     * It will load unloaded classes if matches.
     *
     * @return
     */
    @NotNull Class<?> @NotNull [] classes() throws IOException;

    /**
     * Load the classes with those configurations into the plugin
     *
     * @return
     */
    default @NotNull PluginInfo @NotNull [] load() throws PluginInitializeException, IOException {
        return load((plugin) -> true);
    }
    @NotNull PluginInfo @NotNull [] load(@NotNull Predicate<Class<?>> predicate) throws PluginInitializeException, IOException;

}
