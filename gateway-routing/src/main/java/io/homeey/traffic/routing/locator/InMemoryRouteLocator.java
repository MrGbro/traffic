package io.homeey.traffic.routing.locator;

import io.homeey.traffic.routing.matcher.MethodMatcher;
import io.homeey.traffic.routing.matcher.PathMatcher;
import io.homeey.traffic.routing.model.PredicateDefinition;
import io.homeey.traffic.routing.model.RouteDefinition;
import io.homeey.traffic.routing.request.RouteRequest;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemoryRouteLocator implements RouteLocator {

    private static final String PREDICATE_PATH = "Path";
    private static final String PREDICATE_METHOD = "Method";
    private static final String ARG_VALUE = "value";

    private final List<RouteDefinition> routes;
    private final PathMatcher pathMatcher;
    private final MethodMatcher methodMatcher;

    public InMemoryRouteLocator(List<RouteDefinition> routes) {
        this(routes, new PathMatcher(), new MethodMatcher());
    }

    InMemoryRouteLocator(List<RouteDefinition> routes, PathMatcher pathMatcher, MethodMatcher methodMatcher) {
        this.pathMatcher = pathMatcher;
        this.methodMatcher = methodMatcher;

        List<RouteDefinition> input = routes == null ? List.of() : List.copyOf(routes);
        input.forEach(this::validateRoute);
        this.routes = input.stream()
                .sorted(Comparator.comparingInt(RouteDefinition::order))
                .toList();
    }

    @Override
    public Optional<RouteDefinition> locate(RouteRequest request) {
        if (request == null || request.path() == null || request.method() == null) {
            return Optional.empty();
        }
        return routes.stream()
                .filter(route -> matchesAllPredicates(route, request))
                .findFirst();
    }

    private boolean matchesAllPredicates(RouteDefinition route, RouteRequest request) {
        for (PredicateDefinition predicate : route.predicates()) {
            if (!matchesPredicate(predicate, request)) {
                return false;
            }
        }
        return true;
    }

    private boolean matchesPredicate(PredicateDefinition predicate, RouteRequest request) {
        if (predicate == null || predicate.name() == null) {
            return false;
        }
        String expected = valueArg(predicate.args());
        return switch (predicate.name()) {
            case PREDICATE_PATH -> pathMatcher.matches(request.path(), expected);
            case PREDICATE_METHOD -> methodMatcher.matches(request.method(), expected);
            default -> false;
        };
    }

    private String valueArg(Map<String, String> args) {
        if (args == null) {
            return null;
        }
        return args.get(ARG_VALUE);
    }

    private void validateRoute(RouteDefinition route) {
        if (route == null) {
            throw new IllegalArgumentException("route must not be null");
        }
        if (isBlank(route.id())) {
            throw new IllegalArgumentException("route id must not be blank");
        }
        if (isBlank(route.target())) {
            throw new IllegalArgumentException("route target must not be blank");
        }
        if (route.predicates() == null || route.predicates().isEmpty()) {
            throw new IllegalArgumentException("route predicates must not be empty");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
