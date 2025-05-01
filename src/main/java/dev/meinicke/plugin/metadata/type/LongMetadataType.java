package dev.meinicke.plugin.metadata.type;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LongMetadataType implements MetadataType<Long> {

    private final @Nullable Long fromInclusive, toExclusive;
    private final boolean required;

    public LongMetadataType(@Nullable Long fromInclusive, @Nullable Long toExclusive, boolean required) {
        this.fromInclusive = fromInclusive;
        this.toExclusive = toExclusive;
        this.required = required;
    }

    // Getters

    @Override
    public @NotNull Class<Long> getReference() {
        return long.class;
    }
    @Override
    public boolean isRequired() {
        return required;
    }

    public @Nullable Long getFromInclusive() {
        return fromInclusive;
    }
    public @Nullable Long getToExclusive() {
        return toExclusive;
    }

    // Accept module

    @Override
    public void accept(@Nullable Long l) {
        if (isRequired() && l == null) {
            throw new NullPointerException("the long is null but this type is required to be non-null!");
        }

        // Check if it's under range
        if (l != null) {
            if (getFromInclusive() != null && l < getFromInclusive()) {
                throw new IllegalStateException("the long must be more than or equal to " + getFromInclusive() + ", the current is: " + l);
            } else if (getToExclusive() != null && l >= getToExclusive()) {
                throw new IllegalStateException("the long must be lower than " + getToExclusive() + ", the current is: " + l);
            }
        }
    }

}
