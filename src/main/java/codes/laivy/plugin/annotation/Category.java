package codes.laivy.plugin.annotation;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Repeatable(value = Category.Categories.class)
public @interface Category {
    @NotNull String name();

    // Classes

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @interface Categories {
        @NotNull Category @NotNull [] value();
    }

}