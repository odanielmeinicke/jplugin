package dev.meinicke.plugin.context;

import dev.meinicke.plugin.PluginInfo;
import dev.meinicke.plugin.attribute.Attributes;
import dev.meinicke.plugin.factory.PluginFinder;
import dev.meinicke.plugin.metadata.Metadata;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * Provides contextual information and services to a plugin during its lifecycle.
 * <p>
 * Implementations of this interface are passed to plugin initializers (e.g. {@code MethodPluginInitializer})
 * and plugin execution entry points to give the plugin insight into its own configuration,
 * its peers, and the hosting framework. The context is immutable for the duration of a plugin's
 * initialization to guarantee consistency of metadata and environment.
 * </p>
 *
 * <h3>Core Responsibilities:</h3>
 * <ul>
 *   <li>Identify the plugin being initialized via {@link #getCurrentPlugin()}.</li>
 *   <li>Expose the complete set of loaded plugins via {@link #getAllPlugins()}.</li>
 *   <li>Grant access to compile-time and run-time configuration through {@link #getMetadata()} and {@link #getAttributes()}.</li>
 *   <li>Reveal the caller’s class to support reflective or code-generation scenarios via {@link #getCallerClass()}.</li>
 *   <li>Optionally provide the {@link PluginFinder} used to discover and load plugins via {@link #getFinder()}.</li>
 * </ul>
 *
 * <h3>Thread Safety:</h3>
 * <p>
 * Implementations must guarantee that all returned collections and objects are safe-to-read
 * from within the plugin’s initialization thread. Mutating operations on the framework-side context
 * should not be visible mid-initialization.
 * </p>
 *
 * @see PluginInfo
 * @see Metadata
 * @see Attributes
 * @see PluginFinder
 */
public interface PluginContext {

    /**
     * Returns the {@link PluginInfo} descriptor for the plugin currently being initialized
     * or executed.
     * <p>
     * The {@code PluginInfo} object contains immutable metadata such as plugin name, version,
     * declared dependencies, categories, and priority. Use this method to customize behavior
     * based on the plugin’s identity or declared settings.
     * </p>
     *
     * @return the {@code PluginInfo} of the current plugin; never {@code null}
     */
    @NotNull PluginInfo getCurrentPlugin();

    /**
     * Returns a collection of {@link PluginInfo} descriptors for all plugins that have been
     * discovered and/or loaded in the same application context.
     * <p>
     * The returned collection represents a snapshot of plugin discovery at the time of invocation.
     * Iterating this collection allows a plugin to inspect its peers, enforce inter-plugin policies,
     * or coordinate actions with other modules.
     * </p>
     *
     * @return an immutable collection of all loaded plugins; never {@code null}
     */
    @NotNull Collection<PluginInfo> getAllPlugins();

    /**
     * Provides access to compile-time metadata defined via the {@code @Attribute} annotation
     * on the plugin class.
     * <p>
     * This {@link Metadata} instance contains key-value pairs populated from annotation scanning
     * and may include defaults or values merged from global or category-level settings.
     * It is read-only for the duration of initialization.
     * </p>
     *
     * @return the compile-time metadata for the current plugin; never {@code null}
     */
    @NotNull Metadata getMetadata();

    /**
     * Provides access to run-time attributes that may have been added or modified programmatically
     * during plugin discovery or earlier initialization phases.
     * <p>
     * The {@link Attributes} object supports type-safe retrieval of values (e.g. {@code getAsString()},
     * {@code getAsInteger()}) and may be mutated by the hosting framework before final plugin startup.
     * </p>
     *
     * @return the run-time attributes for the current plugin; never {@code null}
     */
    @NotNull Attributes getAttributes();

    /**
     * Returns the class object of the code that requested plugin initialization.
     * <p>
     * This can be used to determine call-site information, perform security checks,
     * or dynamically register behavior based on the invoking module.
     * In typical usage, this corresponds to the main application class or
     * framework bootstrapper that triggered plugin loading.
     * </p>
     *
     * @return the caller’s {@code Class<?>}; never {@code null}
     */
    @NotNull Class<?> getCallerClass();

    /**
     * Returns the {@link PluginFinder} instance used to locate and load plugins, if available.
     * <p>
     * The {@code PluginFinder} offers fine-grained control over package scanning,
     * classloader scopes, and filtering by category or annotation. If initialization
     * was triggered using the static {@code Plugins.initialize(...)} API, this may return {@code null}.
     * </p>
     *
     * @return the {@code PluginFinder} that created this context, or {@code null} if not applicable
     */
    @Nullable PluginFinder getFinder();

}