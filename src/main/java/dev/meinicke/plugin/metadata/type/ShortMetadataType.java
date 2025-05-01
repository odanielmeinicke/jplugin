package dev.meinicke.plugin.metadata.type;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ShortMetadataType implements MetadataType<Short> {

    private final @Nullable Short fromInclusive, toExclusive;
    private final boolean required;

    public ShortMetadataType(@Nullable Short fromInclusive, @Nullable Short toExclusive, boolean required) {
        this.fromInclusive = fromInclusive;
        this.toExclusive = toExclusive;
        this.required = required;
    }

    // Getters

    @Override
    public @NotNull Class<Short> getReference() {
        return short.class;
    }
    @Override
    public boolean isRequired() {
        return required;
    }

    public @Nullable Short getFromInclusive() {
        return fromInclusive;
    }
    public @Nullable Short getToExclusive() {
        return toExclusive;
    }

    // Accept module

    @Override
    public void accept(@Nullable Short l) {
        if (isRequired() && l == null) {
            throw new NullPointerException("the short is null but this type is required to be non-null!");
        }

        // Check if it's under range
        if (l != null) {
            if (getFromInclusive() != null && l < getFromInclusive()) {
                throw new IllegalStateException("the short must be more than or equal to " + getFromInclusive() + ", the current is: " + l);
            } else if (getToExclusive() != null && l >= getToExclusive()) {
                throw new IllegalStateException("the short must be lower than " + getToExclusive() + ", the current is: " + l);
            }
        }
    }

}
