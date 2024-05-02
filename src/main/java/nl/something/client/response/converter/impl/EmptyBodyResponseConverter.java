package nl.something.client.response.converter.impl;

import nl.something.client.response.converter.ResponseConverter;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/**
 * Doesn't actually convert the response, but throws an exception in case a response is requested and an empty response is being handled.
 * {@link ObjectUtils.Null} receives special handling, in that no exception will be thrown, but instead {@link ObjectUtils#NULL} will be returned.
 */
public class EmptyBodyResponseConverter implements ResponseConverter {
    @Nullable
    @Override
    public <T> T convert(final @NotNull String body, final @Nullable String type, final @NotNull Class<T> targetType) throws IOException {
        if (targetType == ObjectUtils.Null.class) {
            @SuppressWarnings("unchecked")
            final T result = (T) ObjectUtils.NULL;
            return result;
        }
        if (StringUtils.isEmpty(body)) {
            throw new IOException("Response body is absent, though required");
        }
        return null;
    }
}
