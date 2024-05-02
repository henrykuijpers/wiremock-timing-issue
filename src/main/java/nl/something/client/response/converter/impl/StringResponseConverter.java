package nl.something.client.response.converter.impl;

import nl.something.client.response.converter.ResponseConverter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class StringResponseConverter implements ResponseConverter {
    @Nullable
    @Override
    public <T> T convert(@NotNull final String body, @Nullable final String type, @NotNull final Class<T> targetType) {
        if (targetType.isAssignableFrom(String.class)) {
            return targetType.cast(body);
        }
        return null;
    }
}
