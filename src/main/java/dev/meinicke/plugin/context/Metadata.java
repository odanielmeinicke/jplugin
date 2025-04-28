package dev.meinicke.plugin.context;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.AbstractMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Um Map, porém com case insensitive na key para strings
 */
public class Metadata extends AbstractMap<String, Object> {

    private final @NotNull Map<String, Object> shade = new LinkedHashMap<>();

    public Metadata() {
    }

    @Override
    public @Nullable Object put(@NotNull String key, @Nullable Object value) {
        return shade.put(key.toLowerCase(), value);
    }
    @Override
    public @Nullable Object remove(@NotNull Object key) {
        return shade.remove(key.toString().toLowerCase());
    }

    @Override
    public @Nullable Object get(@NotNull Object key) {
        if (!containsKey(key)) {
            throw new NullPointerException("there's no metadata with key '" + key + "'");
        } else {
            return super.get(key.toString().toLowerCase());
        }
    }

    @Override
    public boolean containsKey(@NotNull Object key) {
        return super.containsKey(key.toString().toLowerCase());
    }

    @Override
    public @NotNull Set<Entry<String, Object>> entrySet() {
        return shade.entrySet();
    }

}
