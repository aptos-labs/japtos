package com.aptoslabs.japtos.client;

import com.aptoslabs.japtos.client.dto.HttpResponse;

import java.util.Map;

/**
 * HTTP client interface for making requests to Aptos nodes.
 *
 * <p>This interface defines the contract for HTTP communication with Aptos
 * fullnodes and indexers. It provides methods for both GET and POST requests
 * with support for custom headers and request bodies.</p>
 *
 * <p>The interface is designed to be implementation-agnostic, allowing different
 * HTTP client libraries to be used while maintaining a consistent API.</p>
 *
 * <p>Designed by Aptos Labs.</p>
 *
 * @author @rrigoni
 * @since 1.0.0
 */
public interface HttpClient {

    /**
     * Performs an HTTP GET request to the specified URL.
     *
     * @param url     the URL to send the GET request to
     * @param headers optional headers to include in the request (can be null)
     * @return the HTTP response containing status code and body
     * @throws Exception if the request fails due to network issues or other errors
     */
    HttpResponse get(String url, Map<String, String> headers) throws Exception;

    /**
     * Performs an HTTP POST request to the specified URL with a string body.
     *
     * @param url     the URL to send the POST request to
     * @param headers optional headers to include in the request (can be null)
     * @param body    the request body as a string (typically JSON)
     * @return the HTTP response containing status code and body
     * @throws Exception if the request fails due to network issues or other errors
     */
    HttpResponse post(String url, Map<String, String> headers, String body) throws Exception;

    /**
     * Performs an HTTP POST request to the specified URL with a byte array body.
     *
     * @param url     the URL to send the POST request to
     * @param headers optional headers to include in the request (can be null)
     * @param body    the request body as a byte array (typically BCS-encoded data)
     * @return the HTTP response containing status code and body
     * @throws Exception if the request fails due to network issues or other errors
     */
    HttpResponse post(String url, Map<String, String> headers, byte[] body) throws Exception;
}
