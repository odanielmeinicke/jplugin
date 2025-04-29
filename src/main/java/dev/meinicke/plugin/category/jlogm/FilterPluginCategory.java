package dev.meinicke.plugin.category.jlogm;

import com.jlogm.Filter;
import com.jlogm.Logger;
import com.jlogm.factory.LoggerFactory;
import dev.meinicke.plugin.PluginInfo;
import dev.meinicke.plugin.category.AbstractPluginCategory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class FilterPluginCategory extends AbstractPluginCategory {

    // Static initializers

    private static final @NotNull Logger log = Logger.create(FilterPluginCategory.class);

    // Object

    public FilterPluginCategory() {
        super("JLOGM Filter");
    }

    // Modules

    public boolean accept(@NotNull PluginInfo.Builder builder) {
        builder.priority(-9);
        return true;
    }

    @Override
    public void run(@NotNull PluginInfo info) {
        @Nullable Object instance = info.getInstance();

        if (!(instance instanceof Filter)) {
            log.warn("Cannot automatically register filter '" + info.getReference().getName() + "' because the plugin doesn't have a valid instance!");
        } else {
            @NotNull Filter filter = (Filter) instance;
            LoggerFactory.getInstance().getFilters().add(filter);
        }
    }

}
