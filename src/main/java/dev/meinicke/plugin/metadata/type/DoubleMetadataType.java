package dev.meinicke.plugin.metadata.type;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DoubleMetadataType implements MetadataType<Double> {

    private final @Nullable Double fromInclusive, toExclusive;
    private final boolean required;

    public DoubleMetadataType(@Nullable Double fromInclusive, @Nullable Double toExclusive, boolean required) {
        this.fromInclusive = fromInclusive;
        this.toExclusive = toExclusive;
        this.required = required;
    }

    // Getters

    @Override
    public @NotNull Class<Double> getReference() {
        return double.class;
    }
    @Override
    public boolean isRequired() {
        return required;
    }

    public @Nullable Double getFromInclusive() {
        return fromInclusive;
    }
    public @Nullable Double getToExclusive() {
        return toExclusive;
    }

    // Accept module

    @Override
    public void accept(@Nullable Double l) {
        if (isRequired() && l == null) {
            throw new NullPointerException("the double is null but this type is required to be non-null!");
        }

        // Check if it's under range
        if (l != null) {
            if (getFromInclusive() != null && l < getFromInclusive()) {
                throw new IllegalStateException("the double must be more than or equal to " + getFromInclusive() + ", the current is: " + l);
            } else if (getToExclusive() != null && l > getToExclusive()) {
                throw new IllegalStateException("the double must be lower than " + getToExclusive() + ", the current is: " + l);
            }
        }
    }

}
