package io.homeey.traffic.core;

import io.homeey.traffic.common.Phase;
import io.homeey.traffic.core.context.GatewayContext;
import io.homeey.traffic.core.engine.GatewayEngine;
import io.homeey.traffic.core.engine.route.RoutePhaseFilter;
import io.homeey.traffic.routing.context.RoutingAttributes;
import io.homeey.traffic.spi.context.ExchangeAttributes;
import io.homeey.traffic.routing.locator.InMemoryRouteLocator;
import io.homeey.traffic.routing.model.PredicateDefinition;
import io.homeey.traffic.routing.model.RouteDefinition;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RoutePhaseIntegrationTest {

    @Test
    void shouldContinueToResponseWithRouteAttributesWhenMatched() {
        GatewayEngine engine = new GatewayEngine();
        List<Phase> visited = new ArrayList<>();

        InMemoryRouteLocator locator = new InMemoryRouteLocator(List.of(
                new RouteDefinition(
                        "r-orders",
                        "http://svc-orders",
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
        engine.registerFilter(Phase.RESPONSE, (ctx, chain) -> visited.add(Phase.RESPONSE));

        GatewayContext context = new GatewayContext("req-1");
        context.attribute(ExchangeAttributes.REQUEST_PATH, "/orders");
        context.attribute(ExchangeAttributes.REQUEST_METHOD, "GET");

        engine.execute(context);

        assertThat(context.isTerminated()).isFalse();
        String routeId = context.attribute(RoutingAttributes.ROUTE_ID);
        String routeTarget = context.attribute(RoutingAttributes.ROUTE_TARGET);
        assertThat(routeId).isEqualTo("r-orders");
        assertThat(routeTarget).isEqualTo("http://svc-orders");
        assertThat(visited).containsExactly(Phase.PRE_FORWARD, Phase.RESPONSE);
    }

    @Test
    void shouldTerminateInRouteAndOnlyEnterResponseWhenNotMatched() {
        GatewayEngine engine = new GatewayEngine();
        List<Phase> visited = new ArrayList<>();

        InMemoryRouteLocator locator = new InMemoryRouteLocator(List.of(
                new RouteDefinition(
                        "r-orders",
                        "http://svc-orders",
                        10,
                        List.of(
                                new PredicateDefinition("Path", Map.of("value", "/orders")),
                                new PredicateDefinition("Method", Map.of("value", "GET"))
                        )
                )
        ));

        engine.registerFilter(Phase.ROUTE, (ctx, chain) -> {
            visited.add(Phase.ROUTE);
            new RoutePhaseFilter(locator).filter(ctx, chain);
        });
        engine.registerFilter(Phase.PRE_FORWARD, (ctx, chain) -> {
            visited.add(Phase.PRE_FORWARD);
            chain.filter(ctx);
        });
        engine.registerFilter(Phase.RESPONSE, (ctx, chain) -> visited.add(Phase.RESPONSE));

        GatewayContext context = new GatewayContext("req-1");
        context.attribute(ExchangeAttributes.REQUEST_PATH, "/users");
        context.attribute(ExchangeAttributes.REQUEST_METHOD, "GET");

        engine.execute(context);

        assertThat(context.isTerminated()).isTrue();
        assertThat(context.statusCode()).contains(404);
        assertThat(visited).containsExactly(Phase.ROUTE, Phase.RESPONSE);
    }
}
