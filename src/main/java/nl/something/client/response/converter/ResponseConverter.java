package nl.something.client.response.converter;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public interface ResponseConverter {
    @Nullable
    <T> T convert(@NotNull final String body, @Nullable final String type, @NotNull final Class<T> targetType) throws IOException;
}
