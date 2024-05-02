package nl.something.exceptions;

import lombok.Getter;
import nl.something.client.response.converter.ResponseConverterFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

@Getter
public class RestCallResponseException extends RestCallException {
    private static final long serialVersionUID = 5845779629648662445L;
    private static final String MESSAGE_FORMAT = "Failed to perform REST-call: We got back %d %s with type %s and body %s";

    private final int statusCode;
    private final String statusText;
    private final String contentType;
    private final String body;

    public RestCallResponseException(final int statusCode, @NotNull final String statusText, @Nullable final String contentType, @NotNull final String body) {
        this(statusCode, statusText, contentType, body, null);
    }

    public RestCallResponseException(final int statusCode, @NotNull final String statusText, @Nullable final String contentType, @NotNull final String body,
                                     @Nullable final Throwable cause) {
        super(String.format(MESSAGE_FORMAT, statusCode, statusText, contentType, body), cause);
        this.statusCode = statusCode;
        this.statusText = statusText;
        this.contentType = contentType;
        this.body = body;
    }

    @NotNull
    public <T> T getResponseObject(@NotNull final Class<T> responseType) throws RestCallException {
        try {
            return ResponseConverterFactory.convert(body, contentType, responseType);
        } catch (final IOException e) {
            e.addSuppressed(this);
            throw new RestCallException("Unable to convert response to requested object", e);
        }
    }
}
