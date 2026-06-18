package com.aptoslabs.japtos.client;

import com.aptoslabs.japtos.client.dto.HttpResponse;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link HttpClientImpl} that drive the real OkHttp stack against an
 * in-process {@link MockWebServer}, verifying request construction and response mapping.
 */
class HttpClientImplTest {

    private MockWebServer server;
    private HttpClientImpl client;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        client = new HttpClientImpl();
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    @DisplayName("GET sends headers and maps status/body/headers")
    void get() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200)
                .setHeader("X-Test", "abc").setBody("hello"));

        HttpResponse response = client.get(server.url("/v1").toString(), Map.of("Accept", "application/json"));
        assertEquals(200, response.getStatusCode());
        assertEquals("hello", response.getBody());
        assertTrue(response.isSuccessful());
        // OkHttp lower-cases header names in Headers.toMultimap().
        assertEquals("abc", response.getHeaders().get("x-test"));

        RecordedRequest recorded = server.takeRequest();
        assertEquals("GET", recorded.getMethod());
        assertEquals("application/json", recorded.getHeader("Accept"));
    }

    @Test
    @DisplayName("GET tolerates a null header map and non-2xx responses")
    void getNoHeaders() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(404).setBody("nope"));
        HttpResponse response = client.get(server.url("/missing").toString(), null);
        assertEquals(404, response.getStatusCode());
        assertFalse(response.isSuccessful());
    }

    @Test
    @DisplayName("POST with a string body uses application/json")
    void postString() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));
        HttpResponse response = client.post(server.url("/v1/transactions").toString(),
                Map.of("Accept", "application/json"), "{\"a\":1}");
        assertEquals(200, response.getStatusCode());

        RecordedRequest recorded = server.takeRequest();
        assertEquals("POST", recorded.getMethod());
        assertEquals("{\"a\":1}", recorded.getBody().readUtf8());
        assertTrue(recorded.getHeader("Content-Type").contains("application/json"));
    }

    @Test
    @DisplayName("POST with a null string body sends an empty JSON body")
    void postNullString() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));
        HttpResponse response = client.post(server.url("/v1").toString(), null, (String) null);
        assertEquals(200, response.getStatusCode());
        RecordedRequest recorded = server.takeRequest();
        assertEquals("", recorded.getBody().readUtf8());
    }

    @Test
    @DisplayName("POST with bytes honours an explicit Content-Type header")
    void postBytesWithContentType() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(202).setBody("{\"hash\":\"0x1\"}"));
        byte[] body = {1, 2, 3, 4};
        HttpResponse response = client.post(server.url("/v1/transactions").toString(),
                Map.of("Content-Type", "application/x.aptos.signed_transaction+bcs"), body);
        assertEquals(202, response.getStatusCode());

        RecordedRequest recorded = server.takeRequest();
        assertEquals(4, recorded.getBodySize());
        assertTrue(recorded.getHeader("Content-Type").contains("aptos"));
    }

    @Test
    @DisplayName("POST with bytes defaults to application/x-bcs when no Content-Type is given")
    void postBytesDefaultContentType() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("ok"));
        HttpResponse response = client.post(server.url("/v1").toString(), null, new byte[]{9});
        assertEquals(200, response.getStatusCode());
        RecordedRequest recorded = server.takeRequest();
        assertTrue(recorded.getHeader("Content-Type").contains("application/x-bcs"));
    }

    @Test
    @DisplayName("POST with a null byte body sends an empty payload")
    void postNullBytes() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("ok"));
        HttpResponse response = client.post(server.url("/v1").toString(), Map.of(), (byte[]) null);
        assertEquals(200, response.getStatusCode());
        RecordedRequest recorded = server.takeRequest();
        assertEquals(0, recorded.getBodySize());
    }

    @Test
    @DisplayName("A custom OkHttpClient can be supplied via the constructor")
    void customOkHttpClient() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("custom"));
        HttpClientImpl custom = new HttpClientImpl(new okhttp3.OkHttpClient());
        HttpResponse response = custom.get(server.url("/v1").toString(), null);
        assertEquals("custom", response.getBody());
    }
}
