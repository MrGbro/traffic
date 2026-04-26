package io.homeey.traffic.filter.support;

import io.homeey.traffic.filter.core.GatewayFilter;

public interface OrderedFilter extends GatewayFilter {

    default int order() {
        return 0;
    }
}
