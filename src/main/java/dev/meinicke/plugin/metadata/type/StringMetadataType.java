package dev.meinicke.plugin.metadata.type;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class StringMetadataType implements MetadataType<String> {

    private final @Nullable Integer minLength, maxLength;
    private final boolean required;

    public StringMetadataType(@Nullable Integer minLength, @Nullable Integer maxLength, boolean required) {
        this.minLength = minLength;
        this.maxLength = maxLength;
        this.required = required;
    }

    // Getters

    @Override
    public @NotNull Class<String> getReference() {
        return String.class;
    }
    @Override
    public boolean isRequired() {
        return required;
    }

    public @Nullable Integer getMinLength() {
        return minLength;
    }
    public @Nullable Integer getMaxLength() {
        return maxLength;
    }

    // Accept module

    @Override
    public void accept(@Nullable String string) {
        if (isRequired() && string == null) {
            throw new NullPointerException("The string is null but this value is required to be non-null!");
        }

        if (string != null) {
            int size = string.length();

            if (getMinLength() != null && size < getMinLength()) {
                throw new IllegalStateException("The string length cannot be lower than " + getMinLength() + ", the current is: " + size);
            } else if (getMaxLength() != null && size > getMaxLength()) {
                throw new IllegalStateException("The string length must be lower than or equal to " + getMaxLength() + ", the current is: " + size);
            }
        }
    }


}
