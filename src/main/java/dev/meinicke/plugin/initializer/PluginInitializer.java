package dev.meinicke.plugin.initializer;

import dev.meinicke.plugin.Builder;
import dev.meinicke.plugin.PluginInfo;
import dev.meinicke.plugin.exception.InvalidPluginException;
import org.jetbrains.annotations.NotNull;

/**
 * Defines the contract for constructing a finalized {@link PluginInfo} from a preconfigured
 * {@link Builder}.
 * Implementations of this interface are responsible for validating
 * the builder’s state, applying any final consistency checks, and producing an immutable
 * {@code PluginInfo} instance that accurately represents a plugin’s configuration.
 * <p>
 * This interface serves as the final step in the plugin initialization pipeline: after all
 * metadata, dependencies, categories, and runtime attributes have been gathered and placed
 * into a {@link Builder}, the {@link #build(Builder)} method is invoked
 * to perform the following responsibilities:
 * <ul>
 *   <li><b>Validation:</b> Ensure that all required fields (e.g., plugin identifier, version,
 *       entry point class) are present and meet framework constraints.</li>
 *   <li><b>Normalization:</b> Apply default values or transformations to builder fields if
 *       certain optional metadata were omitted or require standardization (e.g., converting
 *       category identifiers to lowercase, trimming whitespace).</li>
 *   <li><b>Consistency Checks:</b> Verify that declared dependencies exist and do not introduce
 *       circular references, and that category names align with known categories in the system.</li>
 *   <li><b>Final Assembly:</b> Create an immutable {@code PluginInfo} instance whose
 *       properties reflect a fully valid, ready-to-start plugin. Once built, the {@code PluginInfo}
 *       should not allow mutation of any core fields.</li>
 * </ul>
 * <p>
 * A typical usage flow in the plugin framework:
 * <ol>
 *   <li>A {@link PluginInitializer} implementation processes a plugin class (e.g., via annotation
 *       inspection) and produces a {@link Builder} containing metadata, dependencies,
 *       and configuration values.</li>
 *   <li>The caller invokes {@link #build(Builder)} to obtain a {@code PluginInfo}
 *       instance.</li>
 *   <li>Later, when the plugin is started (e.g., {@link PluginInfo#start()} is called), the
 *       framework uses the data in the built {@code PluginInfo} to load classes, resolve
 *       dependencies, and execute the plugin’s entry point.</li>
 * </ol>
 * <p>
 * Because {@code PluginInfo} instances are typically cached or stored for future reference,
 * implementations must ensure thread-safety and immutability of the returned object.
 * Any
 * internal references passed into {@code PluginInfo.Builder} (such as collections of dependencies
 * or category arrays) should be defensively copied or wrapped to prevent accidental external
 * modification after build time.
 * <p>
 * Implementors of this interface may extend the build process to include:
 * <ul>
 *   <li>Registration of plugin-provided services with a service registry.</li>
 *   <li>Emission of lifecycle events (e.g., “pluginPrepared”, “pluginValidated”).</li>
 *   <li>Logging details about the plugin (e.g., version, author, requested permissions).</li>
 * </ul>
 * <p>
 * Once {@link #build(Builder)} returns successfully, the resulting {@link PluginInfo}
 * can be considered a legally constructed and framework-compliant representation of a single
 * plugin module.
 *
 * @author Daniel Meinicke
 * @since 1.0
 */
public interface PluginInitializer {

    /**
     * Converts a preconfigured {@link Builder} into an immutable {@link PluginInfo}
     * instance.
     * This method does <em>not</em> start or activate the plugin; it solely finalizes
     * the builder’s contents, performing validation and normalization steps as needed.
     * <p>
     * <b>Key Responsibilities:</b>
     * <ul>
     *   <li><b>Validation:</b> Throw an {@link IllegalArgumentException} or
     *       {@link InvalidPluginException} if mandatory fields (such as plugin identifier,
     *       main class, or version) are missing or invalid.</li>
     *   <li><b>Dependency Resolution:</b> Ensure that any classes or modules listed as dependencies
     *       in the builder actually exist in the current plugin registry or classpath; if a
     *       dependency is missing or not recognized as a plugin, this method must raise
     *       {@link InvalidPluginException}.</li>
     *   <li><b>Immutability:</b> Return a {@link PluginInfo} whose public API does not allow further
     *       modifications of its core properties. Any collections or maps inside the returned
     *       {@code PluginInfo} must be unmodifiable or defensively copied.</li>
     *   <li><b>Normalization and Defaults:</b> If optional fields (e.g., description, categories) were
     *       omitted in the builder, apply default values or normalize inputs (e.g., trim text,
     *       convert identifiers to lowercase).</li>
     *   <li><b>Consistency Checks:</b> Detect circular or conflicting dependencies, verify that
     *       category names adhere to naming conventions, and ensure no two plugins share the same
     *       unique identifier.</li>
     * </ul>
     * <p>
     * <b>Example Usage:</b>
     * <pre>{@code
     * PluginInfo.Builder builder = PluginInfo.builder()
     *     .withId("com.example.myplugin")
     *     .withVersion("1.2.3")
     *     .withMainClass(MyPlugin.class)
     *     .withDependencies(Arrays.asList(DependencyA.class, DependencyB.class))
     *     .withCategories("data", "visual");
     *
     * PluginInfo pluginInfo = initializer.build(builder);
     * // Later: pluginInfo.start();
     * }</pre>
     *
     * @param builder The {@link Builder} instance that contains all collected
     *                metadata, dependencies, permissions, and runtime attributes for the plugin.
     *                This builder must have been populated (via setters or fluent methods) with
     *                at least the plugin’s unique identifier and main entry point class.
     * @return An immutable {@link PluginInfo} instance. All core fields (ID, version, main class,
     *         categories, dependencies, etc.) must be finalized and unmodifiable. After this call,
     *         any attempt to mutate the returned {@code PluginInfo} should result in an exception
     *         or be outright impossible via its public API.
     * @throws IllegalArgumentException If the {@code builder} is {@code null}, or if required
     *         fields (such as plugin identifier, version, or main class) are missing or invalid.
     * @throws InvalidPluginException   If any validation or consistency check fails (e.g.,
     *         missing dependency, circular reference, duplicate identifier, invalid category name).
     */
    @NotNull PluginInfo build(@NotNull Builder builder) throws InvalidPluginException;
}