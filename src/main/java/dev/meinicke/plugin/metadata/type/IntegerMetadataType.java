package dev.meinicke.plugin.metadata.type;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class IntegerMetadataType implements MetadataType<Integer> {

    private final @Nullable Integer fromInclusive, toExclusive;
    private final boolean required;

    public IntegerMetadataType(@Nullable Integer fromInclusive, @Nullable Integer toExclusive, boolean required) {
        this.fromInclusive = fromInclusive;
        this.toExclusive = toExclusive;
        this.required = required;
    }

    // Getters

    @Override
    public @NotNull Class<Integer> getReference() {
        return int.class;
    }
    @Override
    public boolean isRequired() {
        return required;
    }

    public @Nullable Integer getFromInclusive() {
        return fromInclusive;
    }
    public @Nullable Integer getToExclusive() {
        return toExclusive;
    }

    // Accept module

    @Override
    public void accept(@Nullable Integer integer) {
        if (isRequired() && integer == null) {
            throw new NullPointerException("the integer is null but this type is required to be non-null!");
        }

        // Check if it's under range
        if (integer != null) {
            if (getFromInclusive() != null && integer < getFromInclusive()) {
                throw new IllegalStateException("the integer must be more than or equal to " + getFromInclusive() + ", the current is: " + integer);
            } else if (getToExclusive() != null && integer >= getToExclusive()) {
                throw new IllegalStateException("the integer must be lower than " + getToExclusive() + ", the current is: " + integer);
            }
        }
    }

}