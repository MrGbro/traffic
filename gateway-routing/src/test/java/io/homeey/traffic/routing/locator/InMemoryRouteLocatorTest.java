package io.homeey.traffic.routing.locator;

import io.homeey.traffic.routing.model.PredicateDefinition;
import io.homeey.traffic.routing.model.RouteDefinition;
import io.homeey.traffic.routing.request.RouteRequest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryRouteLocatorTest {

    @Test
    void shouldMatchSingleRoute() {
        InMemoryRouteLocator locator = new InMemoryRouteLocator(List.of(
                route("r1", "http://svc-a", 10, "/orders", "GET")
        ));

        assertThat(locator.locate(new RouteRequest("/orders", "GET")))
                .isPresent()
                .get()
                .extracting(RouteDefinition::id)
                .isEqualTo("r1");
    }

    @Test
    void shouldMatchByLowestOrderFirst() {
        InMemoryRouteLocator locator = new InMemoryRouteLocator(List.of(
                route("r-high", "http://svc-a", 100, "/orders", "GET"),
                route("r-low", "http://svc-b", 10, "/orders", "GET")
        ));

        assertThat(locator.locate(new RouteRequest("/orders", "GET")))
                .isPresent()
                .get()
                .extracting(RouteDefinition::id)
                .isEqualTo("r-low");
    }

    @Test
    void shouldReturnEmptyWhenNoRouteMatched() {
        InMemoryRouteLocator locator = new InMemoryRouteLocator(List.of(
                route("r1", "http://svc-a", 10, "/orders", "GET")
        ));

        assertThat(locator.locate(new RouteRequest("/users", "GET"))).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenRequestMissingPathOrMethod() {
        InMemoryRouteLocator locator = new InMemoryRouteLocator(List.of(
                route("r1", "http://svc-a", 10, "/orders", "GET")
        ));

        assertThat(locator.locate(new RouteRequest(null, "GET"))).isEmpty();
        assertThat(locator.locate(new RouteRequest("/orders", null))).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenPredicateTypeUnknown() {
        RouteDefinition route = new RouteDefinition(
                "r1",
                "http://svc-a",
                10,
                List.of(new PredicateDefinition("Host", Map.of("value", "a.example.com")))
        );

        InMemoryRouteLocator locator = new InMemoryRouteLocator(List.of(route));

        assertThat(locator.locate(new RouteRequest("/orders", "GET"))).isEmpty();
    }

    @Test
    void shouldThrowWhenRouteDefinitionInvalid() {
        assertThatThrownBy(() -> new InMemoryRouteLocator(List.of(
                new RouteDefinition("", "http://svc-a", 10, List.of(pathPredicate("/orders")))
        ))).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new InMemoryRouteLocator(List.of(
                new RouteDefinition("r1", "", 10, List.of(pathPredicate("/orders")))
        ))).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new InMemoryRouteLocator(List.of(
                new RouteDefinition("r1", "http://svc-a", 10, List.of())
        ))).isInstanceOf(IllegalArgumentException.class);
    }

    private static RouteDefinition route(String id, String target, int order, String path, String method) {
        return new RouteDefinition(id, target, order, List.of(pathPredicate(path), methodPredicate(method)));
    }

    private static PredicateDefinition pathPredicate(String path) {
        return new PredicateDefinition("Path", Map.of("value", path));
    }

    private static PredicateDefinition methodPredicate(String method) {
        return new PredicateDefinition("Method", Map.of("value", method));
    }
}
