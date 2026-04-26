package io.homeey.traffic.routing.locator;

import io.homeey.traffic.routing.model.RouteDefinition;
import io.homeey.traffic.routing.request.RouteRequest;

import java.util.Optional;

public interface RouteLocator {

    Optional<RouteDefinition> locate(RouteRequest request);
}
