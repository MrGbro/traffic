package io.homeey.traffic.runtime;

import com.sun.net.httpserver.HttpServer;
import io.homeey.traffic.cluster.discovery.InMemoryServiceDiscovery;
import io.homeey.traffic.cluster.loadbalance.RoundRobinLoadBalancer;
import io.homeey.traffic.forward.http.HttpForwarder;
import io.homeey.traffic.runtime.bootstrap.GatewayBootstrap;
import io.homeey.traffic.routing.locator.InMemoryRouteLocator;
import io.homeey.traffic.routing.model.PredicateDefinition;
import io.homeey.traffic.routing.model.RouteDefinition;
import io.homeey.traffic.transport.jdk.JdkHttpTransportServer;
import io.homeey.traffic.spi.contract.cluster.ServiceInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayBootstrapIntegrationTest {

    @TempDir
    Path tempDir;

    private GatewayBootstrap bootstrap;
    private HttpServer backendA;
    private HttpServer backendB;

    @AfterEach
    void tearDown() {
        if (bootstrap != null) {
            bootstrap.stop();
        }
        if (backendA != null) {
            backendA.stop(0);
        }
        if (backendB != null) {
            backendB.stop(0);
        }
    }

    @Test
    void shouldServeStaticFileForTestingAccess() throws Exception {
        Files.writeString(tempDir.resolve("hello.txt"), "hello-static");

        InMemoryRouteLocator routeLocator = new InMemoryRouteLocator(List.of(
                new RouteDefinition(
                        "r1",
                        "http://127.0.0.1:65535",
                        10,
                        List.of(
                                new PredicateDefinition("Path", Map.of("value", "/orders")),
                                new PredicateDefinition("Method", Map.of("value", "GET"))
                        )
                )
        ));

        bootstrap = new GatewayBootstrap(
                routeLocator,
                serviceName -> List.of(),
                (serviceName, instances) -> java.util.Optional.empty(),
                new HttpForwarder(),
                new JdkHttpTransportServer(tempDir, "/static/")
        );

        int gatewayPort = randomPort();
        bootstrap.start(gatewayPort);

        HttpResponse<byte[]> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + gatewayPort + "/static/hello.txt")).GET().build(),
                HttpResponse.BodyHandlers.ofByteArray()
        );

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(new String(response.body(), StandardCharsets.UTF_8)).isEqualTo("hello-static");
    }

    @Test
    void shouldProxyStaticTarget() throws Exception {
        int backendPort = randomPort();
        backendA = HttpServer.create(new InetSocketAddress(backendPort), 0);
        backendA.createContext("/orders", exchange -> {
            byte[] response = "static-ok".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        backendA.start();

        InMemoryRouteLocator routeLocator = new InMemoryRouteLocator(List.of(
                new RouteDefinition(
                        "r1",
                        "http://127.0.0.1:" + backendPort,
                        10,
                        List.of(
                                new PredicateDefinition("Path", Map.of("value", "/orders")),
                                new PredicateDefinition("Method", Map.of("value", "GET"))
                        )
                )
        ));

        bootstrap = new GatewayBootstrap(
                routeLocator,
                serviceName -> List.of(),
                (serviceName, instances) -> java.util.Optional.empty(),
                new HttpForwarder(),
                new JdkHttpTransportServer()
        );

        int gatewayPort = randomPort();
        bootstrap.start(gatewayPort);

        HttpResponse<byte[]> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + gatewayPort + "/orders")).GET().build(),
                HttpResponse.BodyHandlers.ofByteArray()
        );

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(new String(response.body(), StandardCharsets.UTF_8)).isEqualTo("static-ok");
    }

    @Test
    void shouldProxySvcTargetWithRoundRobin() throws Exception {
        int backendPortA = randomPort();
        int backendPortB = randomPort();

        backendA = HttpServer.create(new InetSocketAddress(backendPortA), 0);
        backendA.createContext("/orders", exchange -> {
            byte[] response = "svc-a".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        backendA.start();

        backendB = HttpServer.create(new InetSocketAddress(backendPortB), 0);
        backendB.createContext("/orders", exchange -> {
            byte[] response = "svc-b".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        backendB.start();

        InMemoryRouteLocator routeLocator = new InMemoryRouteLocator(List.of(
                new RouteDefinition(
                        "r1",
                        "svc://orders",
                        10,
                        List.of(
                                new PredicateDefinition("Path", Map.of("value", "/orders")),
                                new PredicateDefinition("Method", Map.of("value", "GET"))
                        )
                )
        ));

        InMemoryServiceDiscovery discovery = new InMemoryServiceDiscovery(Map.of(
                "orders", List.of(
                        new ServiceInstance("orders", "127.0.0.1", backendPortA, Map.of()),
                        new ServiceInstance("orders", "127.0.0.1", backendPortB, Map.of())
                )
        ));

        bootstrap = new GatewayBootstrap(
                routeLocator,
                discovery,
                new RoundRobinLoadBalancer(),
                new HttpForwarder(),
                new JdkHttpTransportServer()
        );

        int gatewayPort = randomPort();
        bootstrap.start(gatewayPort);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + gatewayPort + "/orders")).GET().build();

        HttpResponse<byte[]> response1 = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        HttpResponse<byte[]> response2 = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

        assertThat(new String(response1.body(), StandardCharsets.UTF_8)).isEqualTo("svc-a");
        assertThat(new String(response2.body(), StandardCharsets.UTF_8)).isEqualTo("svc-b");
    }

    @Test
    void shouldReturn503WhenServiceHasNoInstance() throws Exception {
        InMemoryRouteLocator routeLocator = new InMemoryRouteLocator(List.of(
                new RouteDefinition(
                        "r1",
                        "svc://orders",
                        10,
                        List.of(
                                new PredicateDefinition("Path", Map.of("value", "/orders")),
                                new PredicateDefinition("Method", Map.of("value", "GET"))
                        )
                )
        ));

        bootstrap = new GatewayBootstrap(
                routeLocator,
                new InMemoryServiceDiscovery(Map.of("orders", List.of())),
                new RoundRobinLoadBalancer(),
                new HttpForwarder(),
                new JdkHttpTransportServer()
        );

        int gatewayPort = randomPort();
        bootstrap.start(gatewayPort);

        HttpResponse<byte[]> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + gatewayPort + "/orders")).GET().build(),
                HttpResponse.BodyHandlers.ofByteArray()
        );

        assertThat(response.statusCode()).isEqualTo(503);
        assertThat(new String(response.body(), StandardCharsets.UTF_8)).isEqualTo("Service Unavailable");
    }

    private int randomPort() throws Exception {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
