package nl.something.exceptions;

import java.io.IOException;

public class RestCallException extends IOException {
    private static final long serialVersionUID = 8129605168584918415L;

    public RestCallException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
