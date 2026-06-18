package com.aptoslabs.japtos.testutil;

import com.aptoslabs.japtos.client.HttpClient;
import com.aptoslabs.japtos.client.dto.HttpResponse;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * A deterministic in-memory {@link HttpClient} used to exercise client logic without a live node.
 *
 * <p>It records the URL, headers and body of every request and replays a queue of pre-programmed
 * responses, allowing tests to assert both the request that was issued and the response handling.</p>
 */
public class RecordingHttpClient implements HttpClient {

    /** A captured request. */
    public static final class Request {
        public final String method;
        public final String url;
        public final Map<String, String> headers;
        public final String stringBody;
        public final byte[] byteBody;

        Request(String method, String url, Map<String, String> headers, String stringBody, byte[] byteBody) {
            this.method = method;
            this.url = url;
            this.headers = headers == null ? new HashMap<>() : new HashMap<>(headers);
            this.stringBody = stringBody;
            this.byteBody = byteBody;
        }
    }

    private final Deque<HttpResponse> responses = new ArrayDeque<>();
    private final java.util.List<Request> requests = new java.util.ArrayList<>();

    /** Queues a JSON/text response with the given status code. */
    public RecordingHttpClient enqueue(int status, String body) {
        responses.add(new HttpResponse(status, status >= 200 && status < 300 ? "OK" : "ERR", body, new HashMap<>()));
        return this;
    }

    public Request lastRequest() {
        return requests.get(requests.size() - 1);
    }

    public int requestCount() {
        return requests.size();
    }

    private HttpResponse nextResponse() {
        if (responses.isEmpty()) {
            throw new IllegalStateException("No queued response for request");
        }
        return responses.poll();
    }

    @Override
    public HttpResponse get(String url, Map<String, String> headers) {
        requests.add(new Request("GET", url, headers, null, null));
        return nextResponse();
    }

    @Override
    public HttpResponse post(String url, Map<String, String> headers, String body) {
        requests.add(new Request("POST", url, headers, body, null));
        return nextResponse();
    }

    @Override
    public HttpResponse post(String url, Map<String, String> headers, byte[] body) {
        requests.add(new Request("POST", url, headers, null, body));
        return nextResponse();
    }
}
