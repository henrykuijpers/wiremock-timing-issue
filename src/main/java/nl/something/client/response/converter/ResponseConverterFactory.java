package nl.something.client.response.converter;

import lombok.experimental.UtilityClass;
import nl.something.client.response.converter.impl.EmptyBodyResponseConverter;
import nl.something.client.response.converter.impl.JsonResponseConverter;
import nl.something.client.response.converter.impl.StringResponseConverter;
import nl.something.client.response.converter.impl.XMLResponseConverter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

@UtilityClass
public class ResponseConverterFactory {
    private static final ResponseConverter[] RESPONSE_CONVERTERS = new ResponseConverter[]{
            new EmptyBodyResponseConverter(),
            new JsonResponseConverter(),
            new StringResponseConverter(),
            new XMLResponseConverter()
    };

    @NotNull
    public static <T> T convert(@NotNull final String body, @Nullable final String type, @NotNull final Class<T> targetType) throws IOException {
        for (final ResponseConverter responseConverter : RESPONSE_CONVERTERS) {
            final T response = responseConverter.convert(body, type, targetType);
            if (response != null) {
                return response;
            }
        }
        throw new IOException("Unable to convert response " + body + " with type " + type + " to " + targetType);
    }
}
