package codes.laivy.plugin.initializer;

import codes.laivy.plugin.info.PluginInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Um PluginInitializer representa a forma como as informações de uma classe com a anotação @Plugin
 * são criadas, a classe que implementar essa interface deve haver um construtor declarado sem parâmetros, a visibilidade pode ser qualquer uma.
 *
 * Ele deve cuidar de criar uma instância do PluginInfo que seja funcional e compatível com o resto do sistema
 */
public interface PluginInitializer {
    @NotNull PluginInfo create(@NotNull Class<?> reference, @Nullable String name, @Nullable String description, @NotNull PluginInfo @NotNull [] dependencies, @NotNull String @NotNull [] categories);
}
