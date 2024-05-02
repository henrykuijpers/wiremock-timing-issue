package nl.something.client;

import org.jetbrains.annotations.NotNull;

public class RestClientResponse<T> {
    private final int statusCode;
    private final T value;

    public RestClientResponse(final int statusCode, @NotNull final T value) {
        this.statusCode = statusCode;
        this.value = value;
    }

    public int getStatusCode() {
        return this.statusCode;
    }

    @NotNull
    public T getValue() {
        return value;
    }
}
