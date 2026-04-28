package io.homeey.traffic.transport.jdk;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.homeey.traffic.spi.context.SpiRequestContext;
import io.homeey.traffic.spi.context.SpiResponseContext;
import io.homeey.traffic.spi.contract.transport.TransportRequestHandler;
import io.homeey.traffic.spi.contract.transport.TransportServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;

public class JdkHttpTransportServer implements TransportServer {

    private final Path staticRoot;
    private final String staticUriPrefix;
    private HttpServer server;

    public JdkHttpTransportServer() {
        this(null, "/static/");
    }

    public JdkHttpTransportServer(Path staticRoot, String staticUriPrefix) {
        this.staticRoot = staticRoot == null ? null : staticRoot.toAbsolutePath().normalize();
        this.staticUriPrefix = (staticUriPrefix == null || staticUriPrefix.isBlank()) ? "/static/" : staticUriPrefix;
    }

    @Override
    public void start(int port, TransportRequestHandler handler) throws Exception {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", exchange -> handleExchange(exchange, handler));
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
    }

    @Override
    public void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    private void handleExchange(HttpExchange exchange, TransportRequestHandler handler) throws IOException {
        if (tryServeStatic(exchange)) {
            return;
        }

        SpiRequestContext request = new SpiRequestContext(
                UUID.randomUUID().toString(),
                exchange.getRequestURI().getPath(),
                exchange.getRequestMethod(),
                flattenHeaders(exchange),
                exchange.getRequestBody().readAllBytes(),
                Map.of()
        );

        SpiResponseContext response = handler.handle(request);

        response.headers().forEach((name, value) -> exchange.getResponseHeaders().add(name, value));
        byte[] body = response.body();
        exchange.sendResponseHeaders(response.statusCode(), body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }

    private boolean tryServeStatic(HttpExchange exchange) throws IOException {
        if (staticRoot == null) {
            return false;
        }

        String path = exchange.getRequestURI().getPath();
        if (!path.startsWith(staticUriPrefix)) {
            return false;
        }

        String relative = path.substring(staticUriPrefix.length());
        Path target = staticRoot.resolve(relative).normalize();
        if (!target.startsWith(staticRoot) || !Files.isRegularFile(target)) {
            writeSimpleResponse(exchange, 404, "Not Found", "text/plain");
            return true;
        }

        byte[] content = Files.readAllBytes(target);
        String contentType = detectContentType(target);
        exchange.getResponseHeaders().add("Content-Type", contentType);
        exchange.sendResponseHeaders(200, content.length);
        exchange.getResponseBody().write(content);
        exchange.close();
        return true;
    }

    private String detectContentType(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        if (name.endsWith(".html")) {
            return "text/html; charset=utf-8";
        }
        if (name.endsWith(".css")) {
            return "text/css; charset=utf-8";
        }
        if (name.endsWith(".js")) {
            return "application/javascript; charset=utf-8";
        }
        if (name.endsWith(".json")) {
            return "application/json; charset=utf-8";
        }
        if (name.endsWith(".png")) {
            return "image/png";
        }
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        return "application/octet-stream";
    }

    private void writeSimpleResponse(HttpExchange exchange, int status, String body, String contentType) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", contentType + "; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private Map<String, String> flattenHeaders(HttpExchange exchange) {
        Map<String, String> headers = new LinkedHashMap<>();
        exchange.getRequestHeaders().forEach((name, values) -> {
            if (!values.isEmpty()) {
                headers.put(name, values.get(0));
            }
        });
        return headers;
    }
}
