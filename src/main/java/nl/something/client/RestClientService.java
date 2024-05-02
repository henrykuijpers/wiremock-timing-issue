package nl.something.client;

import lombok.extern.slf4j.Slf4j;
import nl.something.client.response.converter.ResponseConverterFactory;
import nl.something.exceptions.RestCallException;
import nl.something.exceptions.RestCallResponseException;
import nl.something.exceptions.RestCallTimeoutException;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Executes a REST-call optionally using authentication information (username and password or api key).
 * It communicates with a URL that is constructed from the in the OSGI configuration specified base URL, the path that is given by the calling method.
 */
@Slf4j
public class RestClientService {
    private final String baseUrl;
    private CloseableHttpClient httpClient;

    public RestClientService(@NotNull final String baseUrl, final int timeout) throws IOException {
        this.baseUrl = baseUrl;
        final RequestConfig.Builder requestConfigBuilder = RequestConfig.custom();
        if (timeout != -1) {
            requestConfigBuilder.setSocketTimeout(timeout);
            requestConfigBuilder.setConnectionRequestTimeout(timeout);
            requestConfigBuilder.setConnectTimeout(timeout);
        }
        this.httpClient = HttpClients.custom().setDefaultRequestConfig(requestConfigBuilder.build()).build();
    }

    @NotNull
    public <T> RestClientResponse<T> execute(@NotNull final RestClientRequest<T> request) throws RestCallException {
        final String path = request.getPath();
        final String url = baseUrl + "/" + StringUtils.removeStart(path, "/") + determineQueryString(request);

        final RequestBuilder requestBuilder = RequestBuilder.create(request.getMethod()).setUri(url);
        final Object body = request.getBody();
        if (body != null) {
            addRequestBody(requestBuilder, body);
        }
        request.getHeaders().forEach(requestBuilder::addHeader);
        log.info("Opening connection to: {}", url);
        final long startTime = System.currentTimeMillis();
        final HttpContext context = HttpClientContext.create();
        request.getAttributes().forEach(context::setAttribute);
        try (final CloseableHttpResponse httpResponse = httpClient.execute(requestBuilder.build(), context)) {
            final long elapsedTime = System.currentTimeMillis() - startTime;
            log.info(String.format("Total elapsed response time: %dms for %s", elapsedTime, url));
            final int statusCode = httpResponse.getStatusLine().getStatusCode();
            final HttpEntity entity = httpResponse.getEntity();
            final String contentType = Optional
                .ofNullable(entity)
                .map(HttpEntity::getContentType)
                .map(Header::getValue)
                .orElse(null);
            final String responseBody = entity == null ? StringUtils.EMPTY : EntityUtils.toString(entity);
            if (statusCode >= 400) {
                throw new RestCallResponseException(statusCode, httpResponse.getStatusLine().getReasonPhrase(), contentType, responseBody);
            }
            return new RestClientResponse<>(statusCode, ResponseConverterFactory.convert(responseBody, contentType, request.getResponseType()));
        } catch (final RestCallException e) {
            throw e;
        } catch (final ConnectTimeoutException | SocketTimeoutException e) {
            throw new RestCallTimeoutException(url, e);
        } catch (final IOException e) {
            throw new RestCallException("Failed to retrieve the requested data from the api for URL " + url, e);
        }
    }

    @NotNull
    private static <T> String determineQueryString(@NotNull final RestClientRequest<T> request) {
        final List<BasicNameValuePair> nameValuePairs = getNameValuePairs(request);

        if (nameValuePairs.isEmpty()) {
            return StringUtils.EMPTY;
        }

        return "?" + URLEncodedUtils.format(nameValuePairs, StandardCharsets.UTF_8);
    }

    @NotNull
    private static <T> List<BasicNameValuePair> getNameValuePairs(@NotNull final RestClientRequest<T> request) {
        final Map<String, List<String>> parameters = request.getParameters();

        if (parameters.isEmpty()) {
            return Collections.emptyList();
        }

        switch (request.getParameterHandling()) {
            case DUPLICATE:
                return getQueryStringsDuplicate(parameters);
            case INDEXED_ARRAY:
                return getQueryStringsIndexedArray(parameters);
            default:
                return getQueryStringsOverwrite(parameters);
        }
    }

    @NotNull
    private static List<BasicNameValuePair> getQueryStringsOverwrite(final Map<String, List<String>> parameters) {
        return parameters.entrySet()
            .stream()
            .filter(entry -> Optional.ofNullable(entry.getValue())
                .map(values -> !values.isEmpty()).orElse(false))
            .map(entry -> new BasicNameValuePair(entry.getKey(), entry.getValue().get(0)))
            .collect(Collectors.toList());
    }

    @NotNull
    private static List<BasicNameValuePair> getQueryStringsDuplicate(final Map<String, List<String>> parameters) {
        return parameters.entrySet()
            .stream()
            .flatMap(entry -> entry.getValue().stream().map(value -> new BasicNameValuePair(entry.getKey(), value)))
            .collect(Collectors.toList());
    }

    @NotNull
    private static List<BasicNameValuePair> getQueryStringsIndexedArray(final Map<String, List<String>> parameters) {
        final List<BasicNameValuePair> queryStrings = new ArrayList<>();

        for (final Map.Entry<String, List<String>> entry : parameters.entrySet()) {
            final List<String> values = entry.getValue();
            final int parameterSize = values.size();

            if (parameterSize == 1) {
                queryStrings.add(new BasicNameValuePair(entry.getKey(), entry.getValue().get(0)));
            } else if (parameterSize >= 2) {
                for (int i = 0; i < parameterSize; i++) {
                    final String indexedKey = String.format("%s[%d]", entry.getKey(), i);
                    queryStrings.add(new BasicNameValuePair(indexedKey, values.get(i)));
                }
            }
        }

        return queryStrings;
    }

    private static void addRequestBody(@NotNull final RequestBuilder requestBuilder, @NotNull final Object data) {
        if (data instanceof String) {
            requestBuilder.setEntity(new StringEntity((String) data, StandardCharsets.UTF_8));
        } else if (data instanceof byte[]) {
            requestBuilder.setEntity(new ByteArrayEntity((byte[]) data));
        } else {
            requestBuilder.setHeader("Accept", "application/json");
            requestBuilder.setHeader("Content-Type", "application/json");
            requestBuilder.setEntity(new StringEntity(data.toString(), StandardCharsets.UTF_8));
        }
    }
}
