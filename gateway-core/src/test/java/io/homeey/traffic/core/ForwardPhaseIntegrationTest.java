package io.homeey.traffic.core;

import com.sun.net.httpserver.HttpServer;
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

    private HttpServer backend;

    @AfterEach
    void tearDown() {
        if (backend != null) {
            backend.stop(0);
        }
    }

    @Test
    void shouldRunRouteForwardResponseEndToEnd() throws Exception {
        int port = randomPort();
        backend = HttpServer.create(new InetSocketAddress(port), 0);
        backend.createContext("/orders", exchange -> {
            byte[] response = "forward-ok".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("X-Backend", "phase4");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        backend.start();

        GatewayEngine engine = new GatewayEngine();
        List<Phase> visited = new ArrayList<>();

        InMemoryRouteLocator locator = new InMemoryRouteLocator(List.of(
                new RouteDefinition(
                        "r-orders",
                        "http://127.0.0.1:" + port,
                        10,
                        List.of(
                                new PredicateDefinition("Path", Map.of("value", "/orders")),
                                new PredicateDefinition("Method", Map.of("value", "GET"))
                        )
                )
        ));

        engine.registerFilter(Phase.ROUTE, new RoutePhaseFilter(locator));
        engine.registerFilter(Phase.PRE_FORWARD, (ctx, chain) -> {
            visited.add(Phase.PRE_FORWARD);
            chain.filter(ctx);
        });
        engine.registerFilter(Phase.FORWARD, (ctx, chain) -> {
            visited.add(Phase.FORWARD);
            new ForwardPhaseFilter(new HttpForwarder()).filter(ctx, chain);
        });
        engine.registerFilter(Phase.POST_FORWARD, (ctx, chain) -> {
            visited.add(Phase.POST_FORWARD);
            chain.filter(ctx);
        });
        engine.registerFilter(Phase.RESPONSE, (ctx, chain) -> visited.add(Phase.RESPONSE));

        GatewayContext context = new GatewayContext("req-1");
        context.attribute(ExchangeAttributes.REQUEST_PATH, "/orders");
        context.attribute(ExchangeAttributes.REQUEST_METHOD, "GET");
        context.attribute(ExchangeAttributes.REQUEST_HEADERS, Map.of("Accept", "*/*"));
        context.attribute(ExchangeAttributes.REQUEST_BODY, new byte[0]);

        engine.execute(context);

        Integer status = context.attribute(ExchangeAttributes.RESPONSE_STATUS);
        byte[] body = context.attribute(ExchangeAttributes.RESPONSE_BODY);

        assertThat(context.isTerminated()).isFalse();
        assertThat(status).isEqualTo(200);
        assertThat(new String(body, StandardCharsets.UTF_8)).isEqualTo("forward-ok");
        assertThat(visited).containsExactly(
                Phase.PRE_FORWARD,
                Phase.FORWARD,
                Phase.POST_FORWARD,
                Phase.RESPONSE
        );
    }

    @Test
    void shouldTerminate502AndOnlyEnterResponseWhenForwardFails() {
        GatewayEngine engine = new GatewayEngine();
        List<Phase> visited = new ArrayList<>();

        InMemoryRouteLocator locator = new InMemoryRouteLocator(List.of(
                new RouteDefinition(
                        "r-orders",
                        "http://127.0.0.1:9",
                        10,
                        List.of(
                                new PredicateDefinition("Path", Map.of("value", "/orders")),
                                new PredicateDefinition("Method", Map.of("value", "GET"))
                        )
                )
        ));

        engine.registerFilter(Phase.ROUTE, new RoutePhaseFilter(locator));
        engine.registerFilter(Phase.FORWARD, (ctx, chain) -> {
            visited.add(Phase.FORWARD);
            new ForwardPhaseFilter(new HttpForwarder()).filter(ctx, chain);
        });
        engine.registerFilter(Phase.POST_FORWARD, (ctx, chain) -> {
            visited.add(Phase.POST_FORWARD);
            chain.filter(ctx);
        });
        engine.registerFilter(Phase.RESPONSE, (ctx, chain) -> visited.add(Phase.RESPONSE));

        GatewayContext context = new GatewayContext("req-2");
        context.attribute(ExchangeAttributes.REQUEST_PATH, "/orders");
        context.attribute(ExchangeAttributes.REQUEST_METHOD, "GET");
        context.attribute(ExchangeAttributes.REQUEST_HEADERS, Map.of());
        context.attribute(ExchangeAttributes.REQUEST_BODY, new byte[0]);

        engine.execute(context);

        assertThat(context.isTerminated()).isTrue();
        assertThat(context.statusCode()).contains(502);
        assertThat(visited).containsExactly(Phase.FORWARD, Phase.RESPONSE);
    }

    private int randomPort() throws Exception {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
