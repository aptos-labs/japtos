package com.aptoslabs.japtos.client;

import com.aptoslabs.japtos.client.dto.HttpResponse;
import okhttp3.*;
import okhttp3.Request.Builder;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * OkHttp implementation of HttpClient.
 *
 * <p>This class provides a concrete implementation of the HttpClient interface
 * using the OkHttp library. It handles HTTP communication with Aptos nodes, including
 * proper timeout configuration, error handling, and response parsing.</p>
 *
 * <p>The implementation supports both JSON and binary data transmission, with automatic
 * content type detection and proper header management.</p>
 *
 * <p>Designed by Aptos Labs.</p>
 *
 * @author @rrigoni
 * @since 1.0.0
 */
public class HttpClientImpl implements HttpClient {
    private final OkHttpClient client;

    /**
     * Constructs a new HttpClientImpl with default timeout settings.
     *
     * <p>Default configuration includes 30-second timeouts for connect, read, and write operations.</p>
     */
    public HttpClientImpl() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    /**
     * Constructs a new HttpClientImpl with a custom OkHttpClient.
     *
     * @param client the pre-configured OkHttpClient to use
     */
    public HttpClientImpl(OkHttpClient client) {
        this.client = client;
    }

    /**
     * Performs an HTTP GET request to the specified URL.
     *
     * @param url     the URL to send the GET request to
     * @param headers optional headers to include in the request (can be null)
     * @return the HTTP response containing status code and body
     * @throws Exception if the request fails due to network issues or other errors
     */
    @Override
    public HttpResponse get(String url, Map<String, String> headers) throws Exception {
        Builder requestBuilder = new Request.Builder().url(url);

        if (headers != null) {
            for (Map.Entry<String, String> header : headers.entrySet()) {
                requestBuilder.addHeader(header.getKey(), header.getValue());
            }
        }

        try (Response response = client.newCall(requestBuilder.build()).execute()) {
            String body = response.body() != null ? response.body().string() : null;
            return new HttpResponse(
                    response.code(),
                    response.message(),
                    body,
                    response.headers().toMultimap().entrySet().stream()
                            .collect(java.util.stream.Collectors.toMap(
                                    Map.Entry::getKey,
                                    entry -> String.join(", ", entry.getValue())
                            ))
            );
        }
    }

    /**
     * Performs an HTTP POST request to the specified URL with a string body.
     *
     * @param url     the URL to send the POST request to
     * @param headers optional headers to include in the request (can be null)
     * @param body    the request body as a string (typically JSON)
     * @return the HTTP response containing status code and body
     * @throws Exception if the request fails due to network issues or other errors
     */
    @Override
    public HttpResponse post(String url, Map<String, String> headers, String body) throws Exception {
        RequestBody requestBody = body != null ?
                RequestBody.create(body, MediaType.get("application/json")) :
                RequestBody.create("", MediaType.get("application/json"));

        return post(url, headers, requestBody);
    }

    /**
     * Performs an HTTP POST request to the specified URL with a byte array body.
     *
     * @param url     the URL to send the POST request to
     * @param headers optional headers to include in the request (can be null)
     * @param body    the request body as a byte array (typically BCS-encoded data)
     * @return the HTTP response containing status code and body
     * @throws Exception if the request fails due to network issues or other errors
     */
    @Override
    public HttpResponse post(String url, Map<String, String> headers, byte[] body) throws Exception {
        // Get Content-Type from headers, default to application/x-bcs if not specified
        String contentType = headers != null ? headers.get("Content-Type") : "application/x-bcs";
        if (contentType == null) {
            contentType = "application/x-bcs";
        }

        RequestBody requestBody = body != null ?
                RequestBody.create(body, MediaType.get(contentType)) :
                RequestBody.create(new byte[0], MediaType.get(contentType));

        return post(url, headers, requestBody);
    }

    /**
     * Internal helper method to perform HTTP POST requests with a RequestBody.
     *
     * @param url         the URL to send the POST request to
     * @param headers     optional headers to include in the request (can be null)
     * @param requestBody the OkHttp RequestBody to send
     * @return the HTTP response containing status code and body
     * @throws Exception if the request fails due to network issues or other errors
     */
    private HttpResponse post(String url, Map<String, String> headers, RequestBody requestBody) throws Exception {
        Builder requestBuilder = new Request.Builder()
                .url(url)
                .post(requestBody);

        if (headers != null) {
            for (Map.Entry<String, String> header : headers.entrySet()) {
                requestBuilder.addHeader(header.getKey(), header.getValue());
            }
        }

        try (Response response = client.newCall(requestBuilder.build()).execute()) {
            String body = response.body() != null ? response.body().string() : null;
            return new HttpResponse(
                    response.code(),
                    response.message(),
                    body,
                    response.headers().toMultimap().entrySet().stream()
                            .collect(java.util.stream.Collectors.toMap(
                                    Map.Entry::getKey,
                                    entry -> String.join(", ", entry.getValue())
                            ))
            );
        }
    }
}
