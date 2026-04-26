package io.homeey.traffic.filter.core;

@FunctionalInterface
public interface FilterChain {

    void filter(FilterContext context) throws Exception;
}
