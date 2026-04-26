package io.homeey.traffic.core;

import io.homeey.traffic.core.context.GatewayContext;
import io.homeey.traffic.core.engine.route.RoutePhaseFilter;
import io.homeey.traffic.filter.core.FilterChain;
import io.homeey.traffic.routing.context.RoutingAttributes;
import io.homeey.traffic.spi.context.ExchangeAttributes;
import io.homeey.traffic.routing.locator.RouteLocator;
import io.homeey.traffic.routing.model.PredicateDefinition;
import io.homeey.traffic.routing.model.RouteDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class RoutePhaseFilterTest {

    @Test
    void shouldWriteRouteAttributesAndContinueWhenMatched() throws Exception {
        RouteDefinition route = new RouteDefinition(
                "r-orders",
                "http://svc-orders",
                10,
                List.of(new PredicateDefinition("Path", Map.of("value", "/orders")))
        );
        RouteLocator locator = request -> Optional.of(route);
        RoutePhaseFilter filter = new RoutePhaseFilter(locator);

        GatewayContext context = new GatewayContext("req-1");
        context.attribute(ExchangeAttributes.REQUEST_PATH, "/orders");
        context.attribute(ExchangeAttributes.REQUEST_METHOD, "GET");

        AtomicBoolean continued = new AtomicBoolean(false);
        FilterChain chain = ctx -> continued.set(true);

        filter.filter(context, chain);

        assertThat(context.isTerminated()).isFalse();
        Boolean matched = context.attribute(RoutingAttributes.ROUTE_MATCHED);
        String routeId = context.attribute(RoutingAttributes.ROUTE_ID);
        String routeTarget = context.attribute(RoutingAttributes.ROUTE_TARGET);
        assertThat(matched).isEqualTo(Boolean.TRUE);
        assertThat(routeId).isEqualTo("r-orders");
        assertThat(routeTarget).isEqualTo("http://svc-orders");
        assertThat(continued).isTrue();
    }

    @Test
    void shouldTerminateWith404WhenNotMatched() throws Exception {
        RouteLocator locator = request -> Optional.empty();
        RoutePhaseFilter filter = new RoutePhaseFilter(locator);

        GatewayContext context = new GatewayContext("req-1");
        context.attribute(ExchangeAttributes.REQUEST_PATH, "/missing");
        context.attribute(ExchangeAttributes.REQUEST_METHOD, "GET");

        AtomicBoolean continued = new AtomicBoolean(false);
        FilterChain chain = ctx -> continued.set(true);

        filter.filter(context, chain);

        assertThat(context.isTerminated()).isTrue();
        assertThat(context.statusCode()).contains(404);
        assertThat(context.terminateReason()).contains("Route not found");
        assertThat(continued).isFalse();
    }

    @Test
    void shouldTreatMissingRequestPathOrMethodAsNotMatched() throws Exception {
        RouteLocator locator = request -> Optional.empty();
        RoutePhaseFilter filter = new RoutePhaseFilter(locator);

        GatewayContext context = new GatewayContext("req-1");
        context.attribute(ExchangeAttributes.REQUEST_PATH, "/orders");

        filter.filter(context, ctx -> {
        });

        assertThat(context.isTerminated()).isTrue();
        assertThat(context.statusCode()).contains(404);
    }
}
