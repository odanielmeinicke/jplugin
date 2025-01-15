package codes.laivy.plugin.annotation;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.*;

import static codes.laivy.plugin.annotation.Dependency.Dependencies;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Repeatable(value = Dependencies.class)
public @interface Dependency {
    @NotNull Class<?> type();

    // Classes

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @interface Dependencies {
        @NotNull Dependency @NotNull [] value();
    }

}
