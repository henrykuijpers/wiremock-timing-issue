package nl.something.client.response.converter.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import nl.something.client.response.converter.ResponseConverter;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

@Slf4j
public class JsonResponseConverter implements ResponseConverter {
    private static final ObjectMapper mapper = new ObjectMapper();

    @Nullable
    @Override
    public <T> T convert(final @NotNull String body, @Nullable final String type, final @NotNull Class<T> targetType)
            throws IOException {
        final boolean contentTypeJson = type != null && type.contains("application/json");
        if (contentTypeJson || isLegacyJson(body, type)) {
            return mapper.readValue(body, targetType);
        }
        return null;
    }

    private static boolean isLegacyJson(final @NotNull String body, final @Nullable String type) {
        final boolean isLegacyJson = (StringUtils.isEmpty(type) || type.contains("text/plain")) && (body.startsWith("{") || body.startsWith("["));
        if (isLegacyJson) {
            // FIXME [hk 19/apr/2023]: WAAS-5129 Remove legacy JSON case
            log.error("Legacy JSON {} found for type {}, check the call stack", body, type, new IOException());
        }
        return isLegacyJson;
    }

}
