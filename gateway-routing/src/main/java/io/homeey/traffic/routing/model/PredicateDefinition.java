package io.homeey.traffic.routing.model;

import java.util.Map;

public record PredicateDefinition(String name, Map<String, String> args) {

    public PredicateDefinition {
        args = args == null ? Map.of() : Map.copyOf(args);
    }
}
