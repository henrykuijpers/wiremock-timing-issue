package nl.something.exceptions;

public class RestCallTimeoutException extends RestCallException {
    private static final long serialVersionUID = 7319388909477972838L;

    public RestCallTimeoutException(final String url, final Throwable cause) {
        super("The API did not respond within the given timeout for URL " + url, cause);
    }
}
