package com.aptoslabs.japtos.client.dto;

import java.util.Map;

/**
 * Represents an HTTP response.

 */
public class HttpResponse {
    private final int statusCode;
    private final String statusText;
    private final String body;
    private final byte[] bodyBytes;
    private final Map<String, String> headers;
    
    public HttpResponse(int statusCode, String statusText, String body, Map<String, String> headers) {
        this.statusCode = statusCode;
        this.statusText = statusText;
        this.body = body;
        this.bodyBytes = body != null ? body.getBytes() : null;
        this.headers = headers;
    }
    
    public HttpResponse(int statusCode, String statusText, byte[] bodyBytes, Map<String, String> headers) {
        this.statusCode = statusCode;
        this.statusText = statusText;
        this.bodyBytes = bodyBytes;
        this.body = bodyBytes != null ? new String(bodyBytes) : null;
        this.headers = headers;
    }
    
    public int getStatusCode() {
        return statusCode;
    }
    
    public String getStatusText() {
        return statusText;
    }
    
    public String getBody() {
        return body;
    }
    
    public byte[] getBodyBytes() {
        return bodyBytes;
    }
    
    public Map<String, String> getHeaders() {
        return headers;
    }
    
    public boolean isSuccessful() {
        return statusCode >= 200 && statusCode < 300;
    }
}
