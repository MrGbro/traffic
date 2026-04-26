package io.homeey.traffic.forward.http;

import com.sun.net.httpserver.HttpServer;
import io.homeey.traffic.spi.context.SpiRequestContext;
import io.homeey.traffic.spi.context.SpiResponseContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.http.HttpClient;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HttpForwarderTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void shouldForwardGetRequest() throws Exception {
        int port = randomPort();
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/echo", exchange -> {
            byte[] response = "ok-get".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("X-Backend", "mock");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        HttpForwarder forwarder = new HttpForwarder(HttpClient.newHttpClient(), Duration.ofSeconds(2));
        SpiRequestContext request = new SpiRequestContext(
                "req-1",
                "/echo",
                "GET",
                Map.of(),
                new byte[0],
                Map.of()
        );

        SpiResponseContext response = forwarder.forward("http://127.0.0.1:" + port, request);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(new String(response.body(), StandardCharsets.UTF_8)).isEqualTo("ok-get");
        assertThat(response.headers()).containsEntry("x-backend", "mock");
    }

    @Test
    void shouldForwardPostBody() throws Exception {
        int port = randomPort();
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/post", exchange -> {
            byte[] body = exchange.getRequestBody().readAllBytes();
            exchange.sendResponseHeaders(201, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();

        HttpForwarder forwarder = new HttpForwarder(HttpClient.newHttpClient(), Duration.ofSeconds(2));
        SpiRequestContext request = new SpiRequestContext(
                "req-2",
                "/post",
                "POST",
                Map.of("Content-Type", "text/plain"),
                "payload".getBytes(StandardCharsets.UTF_8),
                Map.of()
        );

        SpiResponseContext response = forwarder.forward("http://127.0.0.1:" + port, request);

        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(new String(response.body(), StandardCharsets.UTF_8)).isEqualTo("payload");
    }

    @Test
    void shouldThrowTimeoutWhenBackendTooSlow() throws Exception {
        int port = randomPort();
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/slow", exchange -> {
            try {
                Thread.sleep(300);
                byte[] response = "slow".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, response.length);
                exchange.getResponseBody().write(response);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            } finally {
                exchange.close();
            }
        });
        server.setExecutor(Executors.newSingleThreadExecutor());
        server.start();

        HttpForwarder forwarder = new HttpForwarder(HttpClient.newHttpClient(), Duration.ofMillis(100));
        SpiRequestContext request = new SpiRequestContext(
                "req-3",
                "/slow",
                "GET",
                Map.of(),
                new byte[0],
                Map.of()
        );

        assertThatThrownBy(() -> forwarder.forward("http://127.0.0.1:" + port, request))
                .isInstanceOf(HttpTimeoutException.class);
    }

    private int randomPort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
