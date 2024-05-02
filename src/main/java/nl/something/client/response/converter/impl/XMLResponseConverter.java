package nl.something.client.response.converter.impl;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import nl.something.client.response.converter.ResponseConverter;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.StringReader;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class XMLResponseConverter implements ResponseConverter {
    private static final ConcurrentHashMap<Class<?>, JAXBContext> CONTEXT_MAP = new ConcurrentHashMap<>();

    @Nullable
    @Override
    public <T> T convert(final @NotNull String body, @Nullable final String type, final @NotNull Class<T> targetType) throws IOException {
        if (StringUtils.contains(type, "text/xml")) {
            return deserializeXml(targetType, body);
        }
        return null;
    }

    @NotNull
    public <T> T deserializeXml(@NotNull final Class<T> clazz, @NotNull final String entityString) throws IOException {
        try {
            return convertResponse(entityString, clazz);
        } catch (final JAXBException e) {
            throw new IOException("Failed to unmarshall api response to POJO", e);
        }
    }

    private <T> T convertResponse(final String content, @NotNull final Class<T> clazz) throws JAXBException {
        final JAXBContext jaxbContext = CONTEXT_MAP.computeIfAbsent(clazz, XMLResponseConverter::getKey);
        final Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

        return clazz.cast(unmarshaller.unmarshal(new StreamSource(new StringReader(content))));
    }

    @SneakyThrows(JAXBException.class)
    private static <T> JAXBContext getKey(final Class<T> key) {
        return JAXBContext.newInstance(key);
    }
}