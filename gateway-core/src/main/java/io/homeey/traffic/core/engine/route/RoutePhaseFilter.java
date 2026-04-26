package io.homeey.traffic.core.engine.route;

import io.homeey.traffic.filter.core.FilterChain;
import io.homeey.traffic.filter.core.FilterContext;
import io.homeey.traffic.filter.phase.RouteFilter;
import io.homeey.traffic.routing.context.RoutingAttributes;
import io.homeey.traffic.routing.locator.RouteLocator;
import io.homeey.traffic.routing.model.RouteDefinition;
import io.homeey.traffic.routing.request.RouteRequest;

import java.util.Optional;

public class RoutePhaseFilter implements RouteFilter {

    private static final String REQUEST_PATH_ATTR = "request.path";
    private static final String REQUEST_METHOD_ATTR = "request.method";

    private final RouteLocator routeLocator;

    public RoutePhaseFilter(RouteLocator routeLocator) {
        this.routeLocator = routeLocator;
    }

    @Override
    public void filter(FilterContext context, FilterChain chain) throws Exception {
        String path = context.attribute(REQUEST_PATH_ATTR);
        String method = context.attribute(REQUEST_METHOD_ATTR);

        Optional<RouteDefinition> route = routeLocator.locate(new RouteRequest(path, method));
        if (route.isEmpty()) {
            context.terminate(404, "Route not found");
            return;
        }

        RouteDefinition matched = route.get();
        context.attribute(RoutingAttributes.ROUTE_MATCHED, Boolean.TRUE);
        context.attribute(RoutingAttributes.ROUTE_ID, matched.id());
        context.attribute(RoutingAttributes.ROUTE_TARGET, matched.target());
        chain.filter(context);
    }
}
