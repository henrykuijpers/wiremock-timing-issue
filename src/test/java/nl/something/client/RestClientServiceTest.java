package nl.something.client;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import lombok.Getter;
import lombok.Setter;
import nl.something.exceptions.RestCallException;
import nl.something.exceptions.RestCallResponseException;
import nl.something.exceptions.RestCallTimeoutException;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.json.Json;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.adobe.granite.rest.Constants.CT_JSON;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class RestClientServiceTest {
    // We can't make this field static due to timing issues when doing so
    @RegisterExtension
    static final WireMockExtension wireMock = WireMockExtension.newInstance()
        .options(wireMockConfig()
            .disableOptimizeXmlFactoriesLoading(true)
            .dynamicHttpsPort()
            .dynamicPort())
        .build();

    @Test
    void httpClientGetCalled() throws IOException {
        final RestClientService restClientService = new RestClientService("http://localhost:" + wireMock.getRuntimeInfo().getHttpPort(), -1);
        assertAll(
            () -> {
                wireMock.stubFor(get(urlEqualTo("/get/user/id"))
                    .willReturn(aResponse()
                        .withStatus(SC_OK)
                        .withHeader(CONTENT_TYPE, CT_JSON)
                        .withBody("{\"value\":\"success\"}")));
                final RestClientResponse<TestClass> response =
                    restClientService.execute(RestClientRequest.create(HttpGet.METHOD_NAME, TestClass.class).path("/get/user/id").build());
                assertEquals("success", response.getValue().getValue());
            },
            () -> {
                wireMock.stubFor(put(urlEqualTo("/get/user/id"))
                    .willReturn(aResponse()
                        .withStatus(SC_OK)
                        .withHeader(CONTENT_TYPE, CT_JSON)
                        .withBody("{\"value\":\"success\"}")));
                final RestClientResponse<TestClass> response =
                    restClientService.execute(RestClientRequest.create(HttpPut.METHOD_NAME, TestClass.class).path("/get/user/id").build());
                assertEquals("success", response.getValue().getValue());
            },
            () -> {
                wireMock.stubFor(put(urlEqualTo("/get/user/id"))
                    .withRequestBody(WireMock.equalTo("{\"data\":\"data\"}"))
                    .willReturn(aResponse()
                        .withStatus(SC_OK)
                        .withHeader(CONTENT_TYPE, CT_JSON)
                        .withBody("{\"value\":\"success\"}")));
                final RestClientResponse<TestClass> response =
                    restClientService.execute(RestClientRequest.create(HttpPut.METHOD_NAME, TestClass.class).path("/get/user/id").body("{\"data\":\"data\"}").build());
                assertEquals("success", response.getValue().getValue());
            },
            () -> {
                wireMock.stubFor(post(urlEqualTo("/get/user/id"))
                    .willReturn(aResponse()
                        .withStatus(SC_OK)
                        .withHeader(CONTENT_TYPE, CT_JSON)
                        .withBody("{\"value\":\"success\"}")));
                final RestClientResponse<TestClass> response = restClientService.execute(RestClientRequest.create(HttpPost.METHOD_NAME, TestClass.class).path("/get/user/id").build());
                assertEquals("success", response.getValue().getValue());
            }
        );
    }

    @Test
    void httpClientAddsRequestBodyAndHeaders() throws IOException {
        wireMock.stubFor(put(urlEqualTo("/put/user/id?param=param-value-1"))
            .withHeader("header", equalTo("header-value-1"))
            .withRequestBody(equalTo("{\"body-entry\":\"body-entry-value\"}"))
            .willReturn(aResponse()
                .withStatus(SC_OK)
                .withHeader(CONTENT_TYPE, CT_JSON)
                .withBody("{\"value\":\"success\"}")));
        final RestClientService restClientService = new RestClientService("http://localhost:" + wireMock.getRuntimeInfo().getHttpPort(), -1);
        final RestClientResponse<TestClass> response = restClientService.execute(RestClientRequest
            .create(HttpPut.METHOD_NAME, TestClass.class)
            .path("/put/user/id")
            .body(Json
                .createObjectBuilder()
                .add("body-entry", "body-entry-value")
                .build())
                .parameter("param", List.of("param-value-1"))
            .header("header", "header-value-1")
            .build());
        assertEquals(SC_OK, response.getStatusCode());
        assertEquals("success", response.getValue().getValue());
    }

    @Test
    void httpClientAddsDuplicatedRequestParameters() throws IOException {
        wireMock.stubFor(put(urlEqualTo("/put/user/id?param=param-value-1&param=param-value-2"))
            .willReturn(aResponse()
                .withStatus(SC_OK)
                .withHeader(CONTENT_TYPE, CT_JSON)
                .withBody("{\"value\":\"success\"}")));

        final Map<String, List<String>> params = Collections.singletonMap("param", Arrays.asList("param-value-1", "param-value-2"));

        final RestClientRequest<TestClass> restClientRequest = RestClientRequest.create(HttpPut.METHOD_NAME, TestClass.class)
            .path("/put/user/id")
            .parameters(params)
            .parameterHandling(RestClientParameterHandling.DUPLICATE)
            .build();

        final RestClientService restClientService = new RestClientService("http://localhost:" + wireMock.getRuntimeInfo().getHttpPort(), -1);
        final RestClientResponse<TestClass> response = restClientService.execute(restClientRequest);
        assertEquals(SC_OK, response.getStatusCode());
        assertEquals("success", response.getValue().getValue());
    }

    @Test
    void httpClientAddsIndexedArraydRequestParameters() throws IOException {
        wireMock.stubFor(put(urlEqualTo("/put/user/id?param%5B0%5D=param-value-1&param%5B1%5D=param-value-2"))
            .willReturn(aResponse()
                .withStatus(SC_OK)
                .withHeader(CONTENT_TYPE, CT_JSON)
                .withBody("{\"value\":\"success\"}")));

        final Map<String, List<String>> params = Collections.singletonMap("param", Arrays.asList("param-value-1", "param-value-2"));

        final RestClientRequest<TestClass> restClientRequest = RestClientRequest.create(HttpPut.METHOD_NAME, TestClass.class)
            .path("/put/user/id")
            .parameters(params)
            .parameterHandling(RestClientParameterHandling.INDEXED_ARRAY)
            .build();

        final RestClientService restClientService = new RestClientService("http://localhost:" + wireMock.getRuntimeInfo().getHttpPort(), -1);
        final RestClientResponse<TestClass> response = restClientService.execute(restClientRequest);
        assertEquals(SC_OK, response.getStatusCode());
        assertEquals("success", response.getValue().getValue());
    }

    @Test
    void httpClientOnlyIndexesIndexedArraydRequestParametersWhenThereAreMultipleParameters() throws IOException {
        wireMock.stubFor(put(urlEqualTo("/put/user/id?param=param-value-1"))
            .willReturn(aResponse()
                .withStatus(SC_OK)
                .withHeader(CONTENT_TYPE, CT_JSON)
                .withBody("{\"value\":\"success\"}")));

        final Map<String, List<String>> params = Collections.singletonMap("param", Collections.singletonList("param-value-1"));

        final RestClientRequest<TestClass> restClientRequest = RestClientRequest.create(HttpPut.METHOD_NAME, TestClass.class)
            .path("/put/user/id")
            .parameters(params)
            .parameterHandling(RestClientParameterHandling.INDEXED_ARRAY)
            .build();

        final RestClientService restClientService = new RestClientService("http://localhost:" + wireMock.getRuntimeInfo().getHttpPort(), -1);
        final RestClientResponse<TestClass> response = restClientService.execute(restClientRequest);
        assertEquals(SC_OK, response.getStatusCode());
        assertEquals("success", response.getValue().getValue());
    }

    @Test
    void httpClientAddsOverwrittenRequestParameters() throws IOException {
        wireMock.stubFor(put(urlEqualTo("/put/user/id?param=param-value-1"))
            .willReturn(aResponse()
                .withStatus(SC_OK)
                .withHeader(CONTENT_TYPE, CT_JSON)
                .withBody("{\"value\":\"success\"}")));

        final Map<String, List<String>> params = Collections.singletonMap("param", Arrays.asList("param-value-1", "param-value-2"));

        final RestClientRequest<TestClass> restClientRequest = RestClientRequest.create(HttpPut.METHOD_NAME, TestClass.class)
            .path("/put/user/id")
            .parameters(params)
            .parameterHandling(RestClientParameterHandling.OVERWRITE)
            .build();

        final RestClientService restClientService = new RestClientService("http://localhost:" + wireMock.getRuntimeInfo().getHttpPort(), -1);
        final RestClientResponse<TestClass> response = restClientService.execute(restClientRequest);
        assertEquals(SC_OK, response.getStatusCode());
        assertEquals("success", response.getValue().getValue());
    }

    @Test
    void httpClientRequestParametersShouldDefaultToOverwrite() throws IOException {
        wireMock.stubFor(put(urlEqualTo("/put/user/id?param=param-value-1"))
            .willReturn(aResponse()
                .withStatus(SC_OK)
                .withHeader(CONTENT_TYPE, CT_JSON)
                .withBody("{\"value\":\"success\"}")));

        final Map<String, List<String>> params = Collections.singletonMap("param", Arrays.asList("param-value-1", "param-value-2"));

        final RestClientRequest<TestClass> restClientRequest = RestClientRequest.create(HttpPut.METHOD_NAME, TestClass.class)
            .path("/put/user/id")
            .parameters(params)
            .build();

        final RestClientService restClientService = new RestClientService("http://localhost:" + wireMock.getRuntimeInfo().getHttpPort(), -1);
        final RestClientResponse<TestClass> response = restClientService.execute(restClientRequest);
        assertEquals(SC_OK, response.getStatusCode());
        assertEquals("success", response.getValue().getValue());
    }

    @Test
    void httpClientPostsFiles() throws IOException {
        wireMock.stubFor(post(urlEqualTo("/upload/user/id"))
            .withRequestBody(equalTo("Lorem ipsum dolar et selum"))
            .willReturn(aResponse().withBody("OK")));
        final String textFileContent = "Lorem ipsum dolar et selum";
        final RestClientService restClientService = new RestClientService("http://localhost:" + wireMock.getRuntimeInfo().getHttpPort(), -1);
        final RestClientResponse<String> response =
            restClientService.execute(RestClientRequest.create(HttpPost.METHOD_NAME, String.class).path("/upload/user/id").body(textFileContent.getBytes(StandardCharsets.UTF_8)).build());
        assertEquals("OK", response.getValue());
    }

    @Test
    void timeoutExceptionGetsThrown() throws IOException {
        final RestClientService restClientService = new RestClientService("http://localhost:" + wireMock.getRuntimeInfo().getHttpPort(), 200);
        wireMock.setGlobalFixedDelay(1000); // Make all requests take way longer than 1s

        wireMock.stubFor(get(urlEqualTo("/get/user/id"))
            .willReturn(aResponse()
                .withStatus(SC_NOT_FOUND)
                .withHeader(CONTENT_TYPE, CT_JSON)
                .withBody("NOT FOUND")));
        final RestCallTimeoutException exceptionGet =
            assertThrows(RestCallTimeoutException.class,
                () -> restClientService.execute(RestClientRequest.create(HttpGet.METHOD_NAME, Serializable.class).path("get/user/id").build()));
        assertEquals(
            "The API did not respond within the given timeout for URL http://localhost:" + wireMock.getRuntimeInfo().getHttpPort() + "/get/user/id",
            exceptionGet.getMessage()
        );

        wireMock.stubFor(put(urlEqualTo("/put/user/id"))
            .willReturn(aResponse()
                .withStatus(SC_NOT_FOUND)
                .withHeader(CONTENT_TYPE, CT_JSON)
                .withBody("NOT FOUND")));
        final RestCallTimeoutException exceptionPut =
            assertThrows(RestCallTimeoutException.class, () -> restClientService.execute(RestClientRequest.create(HttpPut.METHOD_NAME, Object.class).path("put/user/id").build()));
        assertEquals(
            "The API did not respond within the given timeout for URL http://localhost:" + wireMock.getRuntimeInfo().getHttpPort() + "/put/user/id",
            exceptionPut.getMessage()
        );

        wireMock.stubFor(post(urlEqualTo("/post/user/id"))
            .willReturn(aResponse()
                .withStatus(SC_NOT_FOUND)
                .withHeader(CONTENT_TYPE, CT_JSON)
                .withBody("NOT FOUND")));
        final RestCallTimeoutException exceptionPost =
            assertThrows(RestCallTimeoutException.class, () -> restClientService.execute(RestClientRequest.create(HttpPost.METHOD_NAME, Object.class).path("post/user/id").build()));
        assertEquals(
            "The API did not respond within the given timeout for URL http://localhost:" + wireMock.getRuntimeInfo().getHttpPort() + "/post/user/id",
            exceptionPost.getMessage()
        );
    }

    @Test
    void notFoundExceptionGetsThrown() throws IOException {
        wireMock.stubFor(get(urlEqualTo("/get/user/id"))
            .willReturn(aResponse()
                .withStatus(SC_NOT_FOUND)
                .withHeader(CONTENT_TYPE, CT_JSON)
                .withBody("NOT FOUND")));
        final RestClientService restClientService = new RestClientService("http://localhost:" + wireMock.getRuntimeInfo().getHttpPort(), -1);
        final RestCallResponseException exceptionGet =
            assertThrows(RestCallResponseException.class,
                () -> restClientService.execute(RestClientRequest.create(HttpGet.METHOD_NAME, Serializable.class).path("get/user/id").build()));
        assertEquals(SC_NOT_FOUND, exceptionGet.getStatusCode());
        assertEquals("Not Found", exceptionGet.getStatusText());
        assertEquals("NOT FOUND", exceptionGet.getBody());
        assertEquals("Failed to perform REST-call: We got back 404 Not Found with type application/json and body NOT FOUND", exceptionGet.getMessage());

        wireMock.stubFor(put(urlEqualTo("/put/user/id"))
            .willReturn(aResponse()
                .withStatus(SC_NOT_FOUND)
                .withHeader(CONTENT_TYPE, CT_JSON)
                .withBody("NOT FOUND")));
        final RestCallResponseException exceptionPut =
            assertThrows(RestCallResponseException.class, () -> restClientService.execute(RestClientRequest.create(HttpPut.METHOD_NAME, Object.class).path("put/user/id").build()));
        assertEquals(SC_NOT_FOUND, exceptionPut.getStatusCode());
        assertEquals("Not Found", exceptionPut.getStatusText());
        assertEquals("NOT FOUND", exceptionPut.getBody());
        assertEquals("Failed to perform REST-call: We got back 404 Not Found with type application/json and body NOT FOUND", exceptionPut.getMessage());

        wireMock.stubFor(post(urlEqualTo("/post/user/id"))
            .willReturn(aResponse()
                .withStatus(SC_NOT_FOUND)
                .withHeader(CONTENT_TYPE, CT_JSON)
                .withBody("NOT FOUND")));
        final RestCallResponseException exceptionPost =
            assertThrows(RestCallResponseException.class, () -> restClientService.execute(RestClientRequest.create(HttpPost.METHOD_NAME, Object.class).path("post/user/id").build()));
        assertEquals(SC_NOT_FOUND, exceptionPost.getStatusCode());
        assertEquals("Not Found", exceptionPost.getStatusText());
        assertEquals("NOT FOUND", exceptionPost.getBody());
        assertEquals("Failed to perform REST-call: We got back 404 Not Found with type application/json and body NOT FOUND", exceptionPost.getMessage());
        assertThrows(RestCallResponseException.class, () -> restClientService.execute(RestClientRequest.create(HttpPost.METHOD_NAME).path("post/user/id").build()));
    }

    @Test
    void forbiddenExceptionGetsThrown() throws IOException {
        wireMock.stubFor(get(urlEqualTo("/get/user/id"))
            .willReturn(aResponse()
                .withStatus(SC_FORBIDDEN)
                .withHeader(CONTENT_TYPE, CT_JSON)
                .withBody("FORBIDDEN")));
        final RestClientService restClientService = new RestClientService("http://localhost:" + wireMock.getRuntimeInfo().getHttpPort(), -1);
        final RestCallResponseException exceptionGet =
            assertThrows(RestCallResponseException.class,
                () -> restClientService.execute(RestClientRequest.create(HttpGet.METHOD_NAME, Serializable.class).path("get/user/id").build()));
        assertEquals(SC_FORBIDDEN, exceptionGet.getStatusCode());
        assertEquals("Forbidden", exceptionGet.getStatusText());
        assertEquals("FORBIDDEN", exceptionGet.getBody());
        assertEquals("Failed to perform REST-call: We got back 403 Forbidden with type application/json and body FORBIDDEN", exceptionGet.getMessage());

        wireMock.stubFor(put(urlEqualTo("/put/user/id"))
            .willReturn(aResponse()
                .withStatus(SC_FORBIDDEN)
                .withHeader(CONTENT_TYPE, CT_JSON)
                .withBody("FORBIDDEN")));
        final RestCallResponseException exceptionPut =
            assertThrows(RestCallResponseException.class, () -> restClientService.execute(RestClientRequest.create(HttpPut.METHOD_NAME, Object.class).path("put/user/id").build()));
        assertEquals(SC_FORBIDDEN, exceptionPut.getStatusCode());
        assertEquals("Forbidden", exceptionPut.getStatusText());
        assertEquals("FORBIDDEN", exceptionPut.getBody());
        assertEquals("Failed to perform REST-call: We got back 403 Forbidden with type application/json and body FORBIDDEN", exceptionPut.getMessage());

        wireMock.stubFor(post(urlEqualTo("/post/user/id"))
            .willReturn(aResponse()
                .withStatus(SC_FORBIDDEN)
                .withHeader(CONTENT_TYPE, CT_JSON)
                .withBody("FORBIDDEN")));
        final RestCallResponseException exceptionPost =
            assertThrows(RestCallResponseException.class, () -> restClientService.execute(RestClientRequest.create(HttpPost.METHOD_NAME).path("post/user/id").build()));
        assertEquals(SC_FORBIDDEN, exceptionPost.getStatusCode());
        assertEquals("Forbidden", exceptionPost.getStatusText());
        assertEquals("FORBIDDEN", exceptionPost.getBody());
        assertEquals("Failed to perform REST-call: We got back 403 Forbidden with type application/json and body FORBIDDEN", exceptionPost.getMessage());
        assertThrows(RestCallResponseException.class, () -> restClientService.execute(RestClientRequest.create(HttpPost.METHOD_NAME).path("post/user/id").build()));
    }

    @Test
    void simpleServiceTimeoutConfigurations() throws IOException {
        wireMock.stubFor(get(urlEqualTo("/api/timeout"))
            .willReturn(aResponse()
                .withStatus(SC_OK)
                .withFixedDelay(800)
                .withBody("success")));
        wireMock.stubFor(get(urlEqualTo("/api/no-timeout"))
            .willReturn(aResponse()
                .withStatus(SC_OK)
                .withBody("success")));

        final RestClientService service = new RestClientService("http://localhost:" + wireMock.getRuntimeInfo().getHttpPort(), 500);

        // do not expect a timeout
        final RestClientResponse<String> response = service.execute(RestClientRequest.create(HttpGet.METHOD_NAME, String.class).path("/api/no-timeout").build());
        assertEquals("success", response.getValue());

        // expect a timeout
        final RestCallTimeoutException timeoutException = assertThrows(RestCallTimeoutException.class,
            () -> service.execute(RestClientRequest.create(HttpGet.METHOD_NAME, String.class).path("/api/timeout").build()));
        assertEquals(
            "The API did not respond within the given timeout for URL http://localhost:" + wireMock.getRuntimeInfo().getHttpPort() + "/api/timeout",
            timeoutException.getMessage()
        );
    }

    @Test
    void responseEntityCanBeReadInErrorContext() throws IOException {
        final RestClientService restClientService = new RestClientService("http://localhost:" + wireMock.getRuntimeInfo().getHttpPort(), -1);
        wireMock.stubFor(get(urlEqualTo("/get/user/id"))
            .willReturn(aResponse()
                .withStatus(SC_NOT_FOUND)
                .withHeader(CONTENT_TYPE, CT_JSON)
                .withBody("{\"value\":\"API says: not found :-(\"}")));
        final RestCallResponseException e = assertThrows(RestCallResponseException.class, () ->
            restClientService.execute(RestClientRequest.create(HttpGet.METHOD_NAME, TestClass.class).path("/get/user/id").build()));
        assertEquals("Failed to perform REST-call: We got back 404 Not Found with type application/json and body {\"value\":\"API says: not found :-(\"}", e.getMessage());
        assertEquals(SC_NOT_FOUND, e.getStatusCode());
        assertEquals("Not Found", e.getStatusText());
        assertEquals("application/json", e.getContentType());
        assertEquals("{\"value\":\"API says: not found :-(\"}", e.getBody());
        assertEquals("API says: not found :-(", e.getResponseObject(TestClass.class).getValue());
    }

    @Test
    void responseEntityThatCanBeReadInErrorContextThrowsException() throws IOException {
        final RestClientService restClientService = new RestClientService("http://localhost:" + wireMock.getRuntimeInfo().getHttpPort(), -1);
        wireMock.stubFor(get(urlEqualTo("/get/user/id"))
            .willReturn(aResponse()
                .withStatus(500)
                .withHeader(CONTENT_TYPE, CT_JSON)
                .withBody("{\"BOOM\"}")));
        final RestCallResponseException e = assertThrows(RestCallResponseException.class, () ->
            restClientService.execute(RestClientRequest.create(HttpGet.METHOD_NAME, TestClass.class).path("/get/user/id").build()));
        assertEquals("Failed to perform REST-call: We got back 500 Server Error with type application/json and body {\"BOOM\"}", e.getMessage());
        assertEquals(SC_INTERNAL_SERVER_ERROR, e.getStatusCode());
        assertEquals("Server Error", e.getStatusText());
        assertEquals("application/json", e.getContentType());
        assertEquals("{\"BOOM\"}", e.getBody());
        final RestCallException x = assertThrows(RestCallException.class, () -> e.getResponseObject(TestClass.class));
        assertEquals("Unable to convert response to requested object", x.getMessage());
        assertNotNull(x.getCause());
    }

    @Test
    void responseEntityThatCanBeReadInErrorContextThrowsExceptionx() throws IOException {
        final RestClientService restClientService = new RestClientService("http://localhost:" + wireMock.getRuntimeInfo().getHttpPort(), -1);
        wireMock.stubFor(get(urlEqualTo("/get/user/id"))
            .willReturn(aResponse()
                .withFault(Fault.EMPTY_RESPONSE)));
        final RestCallException e = assertThrows(RestCallException.class, () ->
            restClientService.execute(RestClientRequest.create(HttpGet.METHOD_NAME, TestClass.class).path("/get/user/id").build()));
        assertEquals("Failed to retrieve the requested data from the api for URL http://localhost:" + wireMock.getRuntimeInfo().getHttpPort() + "/get/user/id", e.getMessage());
        final Throwable cause = e.getCause();
        assertThat(cause, is(instanceOf(NoHttpResponseException.class)));
        assertEquals("localhost:" + wireMock.getRuntimeInfo().getHttpPort() + " failed to respond", cause.getMessage());
    }

    @Setter
    @Getter
    public static class TestClass {
        private String value;
    }
}
