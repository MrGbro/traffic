package io.homeey.traffic.filter.core;

import java.util.List;

public final class DefaultFilterChain implements FilterChain {

    private final List<GatewayFilter> filters;
    private final int index;

    public DefaultFilterChain(List<GatewayFilter> filters) {
        this(filters, 0);
    }

    private DefaultFilterChain(List<GatewayFilter> filters, int index) {
        this.filters = List.copyOf(filters);
        this.index = index;
    }

    @Override
    public void filter(FilterContext context) throws Exception {
        if (index >= filters.size()) {
            return;
        }
        GatewayFilter current = filters.get(index);
        FilterChain next = new DefaultFilterChain(filters, index + 1);
        current.filter(context, next);
    }
}
