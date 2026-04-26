package io.homeey.traffic.routing.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RouteDefinitionTest {

    @Test
    void shouldDefensivelyCopyPredicates() {
        List<PredicateDefinition> source = new ArrayList<>();
        source.add(new PredicateDefinition("Path", Map.of("value", "/orders")));

        RouteDefinition route = new RouteDefinition("r1", "http://svc", 1, source);

        source.clear();
        assertThat(route.predicates()).hasSize(1);
        assertThatThrownBy(() -> route.predicates().add(new PredicateDefinition("Method", Map.of("value", "GET"))))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
