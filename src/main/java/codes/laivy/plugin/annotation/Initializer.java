package codes.laivy.plugin.annotation;

import codes.laivy.plugin.initializer.PluginInitializer;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Initializer {
    @NotNull Class<? extends PluginInitializer> type();
}