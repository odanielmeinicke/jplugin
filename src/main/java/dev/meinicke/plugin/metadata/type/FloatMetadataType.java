package dev.meinicke.plugin.metadata.type;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FloatMetadataType implements MetadataType<Float> {

    private final @Nullable Float fromInclusive, toExclusive;
    private final boolean required;

    public FloatMetadataType(@Nullable Float fromInclusive, @Nullable Float toExclusive, boolean required) {
        this.fromInclusive = fromInclusive;
        this.toExclusive = toExclusive;
        this.required = required;
    }

    // Getters

    @Override
    public @NotNull Class<Float> getReference() {
        return float.class;
    }
    @Override
    public boolean isRequired() {
        return required;
    }

    public @Nullable Float getFromInclusive() {
        return fromInclusive;
    }
    public @Nullable Float getToExclusive() {
        return toExclusive;
    }

    // Accept module

    @Override
    public void accept(@Nullable Float l) {
        if (isRequired() && l == null) {
            throw new NullPointerException("the float is null but this type is required to be non-null!");
        }

        // Check if it's under range
        if (l != null) {
            if (getFromInclusive() != null && l < getFromInclusive()) {
                throw new IllegalStateException("the float must be more than or equal to " + getFromInclusive() + ", the current is: " + l);
            } else if (getToExclusive() != null && l >= getToExclusive()) {
                throw new IllegalStateException("the float must be lower than " + getToExclusive() + ", the current is: " + l);
            }
        }
    }

}
