package io.homeey.traffic.filter.core;

public final class FilterInvoker {

    public void invoke(GatewayFilter filter, FilterContext context, FilterChain chain) throws Exception {
        filter.filter(context, chain);
    }
}
