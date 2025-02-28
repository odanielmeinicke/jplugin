package codes.laivy.plugin.category.jlogm;

import codes.laivy.plugin.PluginInfo;
import codes.laivy.plugin.category.AbstractPluginCategory;
import com.jlogm.Filter;
import com.jlogm.Logger;
import com.jlogm.factory.LoggerFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static codes.laivy.plugin.PluginInfo.Builder;

final class FilterPluginCategory extends AbstractPluginCategory {

    // Static initializers

    private static final @NotNull Logger log = Logger.create(FilterPluginCategory.class);

    // Object

    public FilterPluginCategory() {
        super("JLOGM Filter");
    }

    // Modules

    public boolean accept(@NotNull Builder builder) {
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
