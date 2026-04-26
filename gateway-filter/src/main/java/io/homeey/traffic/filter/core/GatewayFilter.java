package io.homeey.traffic.filter.core;

@FunctionalInterface
public interface GatewayFilter {

    void filter(FilterContext context, FilterChain chain) throws Exception;
}
