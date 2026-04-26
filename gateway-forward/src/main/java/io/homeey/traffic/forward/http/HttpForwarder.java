package io.homeey.traffic.forward.http;

import io.homeey.traffic.spi.context.SpiRequestContext;
import io.homeey.traffic.spi.context.SpiResponseContext;
import io.homeey.traffic.spi.contract.forward.Forwarder;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

public class HttpForwarder implements Forwarder {

    private final HttpClient httpClient;
    private final Duration requestTimeout;

    public HttpForwarder() {
        this(HttpClient.newBuilder().build(), Duration.ofSeconds(3));
    }

    public HttpForwarder(HttpClient httpClient, Duration requestTimeout) {
        this.httpClient = httpClient;
        this.requestTimeout = requestTimeout;
    }

    @Override
    public SpiResponseContext forward(String target, SpiRequestContext request) throws IOException, InterruptedException {
        URI uri = URI.create(target).resolve(request.path());
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                .timeout(requestTimeout)
                .method(
                        request.method(),
                        request.body().length == 0
                                ? HttpRequest.BodyPublishers.noBody()
                                : HttpRequest.BodyPublishers.ofByteArray(request.body())
                );

        request.headers().forEach(builder::header);

        HttpResponse<byte[]> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());

        Map<String, String> headers = new LinkedHashMap<>();
        response.headers().map().forEach((name, values) -> {
            if (!values.isEmpty()) {
                headers.put(name, values.get(0));
            }
        });

        return new SpiResponseContext(response.statusCode(), headers, response.body());
    }
}
