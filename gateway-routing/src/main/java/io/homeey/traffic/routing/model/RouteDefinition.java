package io.homeey.traffic.routing.model;

import java.util.List;

public record RouteDefinition(String id, String target, int order, List<PredicateDefinition> predicates) {

    public RouteDefinition {
        predicates = predicates == null ? List.of() : List.copyOf(predicates);
    }
}
