package nl.something.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import org.apache.commons.lang3.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
@Builder
public class RestClientRequest<T> {
    /**
     * The HttpMethod to execute on client (either "GET, "POST", "PUT", "DELETE")
     */
    @NotNull
    private final String method;
    /**
     * The path of the request
     */
    @NotNull
    private final String path;
    /**
     * The object type that is returned from the request
     */
    @NotNull
    private final Class<T> responseType;
    /**
     * The object that is passed along with the request (converted to Json data)
     */
    @Nullable
    private final Object body;
    /**
     * The query parameters that are appended to the path
     */
    @Singular
    private final Map<String, List<String>> parameters;
    /**
     * The additional http headers that are passed along with the request
     */
    @Singular
    private final Map<String, String> headers;

    /**
     * Optional attributes that can contain data for backend processing (will not be sent in the HTTP request)
     */
    @Singular
    private final Map<String, Object> attributes;

    @Builder.Default
    private final RestClientParameterHandling parameterHandling = RestClientParameterHandling.OVERWRITE;

    /**
     * Build a request with the given method name
     *
     * @param method The method for the request to be built
     * @param responseType The object type that is returned from the request
     *
     * @return A RestClient request builder initialized with the given HTTP method and response type
     */
    public static <T> RestClientRequestBuilder<T> create(@NotNull final String method, @NotNull final Class<T> responseType) {
        return new RestClientRequestBuilder<T>().method(method).responseType(responseType);
    }

    /**
     * Build a request with the given method name, indicating that an eventual response is not interesting
     *
     * @param method The method for the request to be built
     * @return A RestClient request builder initialized with the given HTTP method and null response type
     */
    public static RestClientRequestBuilder<ObjectUtils.Null> create(final String method) {
        return create(method, ObjectUtils.Null.class);
    }

    public static class RestClientRequestBuilder<T> {
        /**
         * Adds a single item to the parameter list.
         *
         * If called multiple times will overwrite previous values.
         *
         * Due to lombok Singular being finicky, this method can't be named parameter(String, String). This is because lombok doesn't support overloading of
         * singular annotated fields. Thus we have to name this singularParameter.
         *
         * @param parameterKey used in the parameter
         * @param parameterValue used in the parameter
         * @return builder.
         */
        public RestClientRequestBuilder<T> singularParameter(final String parameterKey, final String parameterValue) {
            return parameter(parameterKey, Collections.singletonList(parameterValue));
        }

        /**
         * Adds multiple items to the builder parameter.
         *
         * If called multiple times will overwrite previous values.
         *
         * Due to lombok Singular being finicky, this method can't be named parameters(Map<String, String>). This is because lombok doesn't support overloading
         * of singular annotated fields. Thus we have to name this singularParameters.
         *
         * @param parameters map of parameters to add
         * @return builder.
         */
        public RestClientRequestBuilder<T> singularParameters(final Map<String, String> parameters) {
            return parameters(parameters.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> Collections.singletonList(entry.getValue()))));
        }
    }
}