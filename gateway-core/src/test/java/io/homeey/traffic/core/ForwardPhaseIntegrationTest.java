package io.homeey.traffic.core;

import com.sun.net.httpserver.HttpServer;
import io.homeey.traffic.cluster.discovery.InMemoryServiceDiscovery;
import io.homeey.traffic.cluster.loadbalance.RoundRobinLoadBalancer;
import io.homeey.traffic.common.Phase;
import io.homeey.traffic.core.context.GatewayContext;
import io.homeey.traffic.core.engine.GatewayEngine;
import io.homeey.traffic.core.engine.forward.ForwardPhaseFilter;
import io.homeey.traffic.core.engine.route.RoutePhaseFilter;
import io.homeey.traffic.forward.http.HttpForwarder;
import io.homeey.traffic.routing.model.PredicateDefinition;
import io.homeey.traffic.routing.model.RouteDefinition;
import io.homeey.traffic.routing.locator.InMemoryRouteLocator;
import io.homeey.traffic.spi.context.ExchangeAttributes;
import io.homeey.traffic.spi.contract.cluster.ServiceInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ForwardPhaseIntegrationTest {

    private HttpServer backendA;
    private HttpServer backendB;

    @AfterEach
    void tearDown() {
        if (backendA != null) {
            backendA.stop(0);
        }
        if (backendB != null) {
            backendB.stop(0);
        }
    }

    @Test
    void shouldRunRouteClusterForwardResponseEndToEnd() throws Exception {
        int portA = randomPort();
        int portB = randomPort();

        backendA = HttpServer.create(new InetSocketAddress(portA), 0);
        backendA.createContext("/orders", exchange -> {
            byte[] response = "from-a".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        backendA.start();

        backendB = HttpServer.create(new InetSocketAddress(portB), 0);
        backendB.createContext("/orders", exchange -> {
            byte[] response = "from-b".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        backendB.start();

        var discovery = new InMemoryServiceDiscovery(Map.of(
                "orders", List.of(
                        new ServiceInstance("orders", "127.0.0.1", portA, Map.of()),
                        new ServiceInstance("orders", "127.0.0.1", portB, Map.of())
                )
        ));
        var lb = new RoundRobinLoadBalancer();

        GatewayEngine engine = buildEngine("svc://orders", discovery, lb);

        GatewayContext ctx1 = baseRequest("req-1");
        engine.execute(ctx1);
        byte[] body1 = ctx1.attribute(ExchangeAttributes.RESPONSE_BODY);

        GatewayContext ctx2 = baseRequest("req-2");
        engine.execute(ctx2);
        byte[] body2 = ctx2.attribute(ExchangeAttributes.RESPONSE_BODY);

        assertThat(ctx1.isTerminated()).isFalse();
        assertThat(ctx2.isTerminated()).isFalse();
        assertThat(new String(body1, StandardCharsets.UTF_8)).isEqualTo("from-a");
        assertThat(new String(body2, StandardCharsets.UTF_8)).isEqualTo("from-b");
    }

    @Test
    void shouldTerminate503AndOnlyEnterResponseWhenNoServiceInstance() {
        var discovery = new InMemoryServiceDiscovery(Map.of("orders", List.of()));
        var lb = new RoundRobinLoadBalancer();

        GatewayEngine engine = buildEngine("svc://orders", discovery, lb);

        List<Phase> visited = new ArrayList<>();
        engine.registerFilter(Phase.POST_FORWARD, (ctx, chain) -> {
            visited.add(Phase.POST_FORWARD);
            chain.filter(ctx);
        });
        engine.registerFilter(Phase.RESPONSE, (ctx, chain) -> visited.add(Phase.RESPONSE));

        GatewayContext context = baseRequest("req-3");
        engine.execute(context);

        assertThat(context.isTerminated()).isTrue();
        assertThat(context.statusCode()).contains(503);
        assertThat(visited).containsExactly(Phase.RESPONSE);
    }

    private GatewayEngine buildEngine(String routeTarget,
                                      InMemoryServiceDiscovery discovery,
                                      RoundRobinLoadBalancer lb) {
        GatewayEngine engine = new GatewayEngine();

        InMemoryRouteLocator locator = new InMemoryRouteLocator(List.of(
                new RouteDefinition(
                        "r-orders",
                        routeTarget,
                        10,
                        List.of(
                                new PredicateDefinition("Path", Map.of("value", "/orders")),
                                new PredicateDefinition("Method", Map.of("value", "GET"))
                        )
                )
        ));

        engine.registerFilter(Phase.ROUTE, new RoutePhaseFilter(locator));
        engine.registerFilter(Phase.FORWARD, (ctx, chain) ->
                new ForwardPhaseFilter(new HttpForwarder(), discovery, lb).filter(ctx, chain));
        engine.registerFilter(Phase.RESPONSE, (ctx, chain) -> chain.filter(ctx));

        return engine;
    }

    private GatewayContext baseRequest(String requestId) {
        GatewayContext context = new GatewayContext(requestId);
        context.attribute(ExchangeAttributes.REQUEST_PATH, "/orders");
        context.attribute(ExchangeAttributes.REQUEST_METHOD, "GET");
        context.attribute(ExchangeAttributes.REQUEST_HEADERS, Map.of("Accept", "*/*"));
        context.attribute(ExchangeAttributes.REQUEST_BODY, new byte[0]);
        return context;
    }

    private int randomPort() throws Exception {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
