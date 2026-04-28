package io.homeey.traffic.transport.jdk;

import io.homeey.traffic.spi.context.SpiResponseContext;
import io.homeey.traffic.spi.contract.transport.TransportRequestHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JdkHttpTransportServerTest {

    @TempDir
    Path tempDir;

    private JdkHttpTransportServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    void shouldMapHttpRequestAndWriteResponse() throws Exception {
        int port = randomPort();
        server = new JdkHttpTransportServer();

        TransportRequestHandler handler = request -> {
            assertThat(request.path()).isEqualTo("/echo");
            assertThat(request.method()).isEqualTo("POST");
            assertThat(new String(request.body(), StandardCharsets.UTF_8)).isEqualTo("hello");
            return new SpiResponseContext(201, Map.of("X-Gateway", "ok"), "world".getBytes(StandardCharsets.UTF_8));
        };

        server.start(port, handler);

        HttpRequest httpRequest = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/echo"))
                .POST(HttpRequest.BodyPublishers.ofString("hello"))
                .build();

        HttpResponse<byte[]> response = HttpClient.newHttpClient().send(httpRequest, HttpResponse.BodyHandlers.ofByteArray());

        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(response.headers().firstValue("X-Gateway")).contains("ok");
        assertThat(new String(response.body(), StandardCharsets.UTF_8)).isEqualTo("world");
    }

    @Test
    void shouldServeStaticFileBeforeDynamicHandler() throws Exception {
        Files.writeString(tempDir.resolve("index.html"), "<h1>hello</h1>");

        int port = randomPort();
        server = new JdkHttpTransportServer(tempDir, "/static/");

        TransportRequestHandler handler = request ->
                new SpiResponseContext(500, Map.of(), "dynamic".getBytes(StandardCharsets.UTF_8));

        server.start(port, handler);

        HttpResponse<byte[]> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/static/index.html")).GET().build(),
                HttpResponse.BodyHandlers.ofByteArray()
        );

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(new String(response.body(), StandardCharsets.UTF_8)).isEqualTo("<h1>hello</h1>");
    }

    @Test
    void shouldReturn404ForMissingStaticFile() throws Exception {
        int port = randomPort();
        server = new JdkHttpTransportServer(tempDir, "/static/");
        server.start(port, request -> new SpiResponseContext(200, Map.of(), new byte[0]));

        HttpResponse<byte[]> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/static/missing.html")).GET().build(),
                HttpResponse.BodyHandlers.ofByteArray()
        );

        assertThat(response.statusCode()).isEqualTo(404);
    }

    private int randomPort() throws Exception {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
