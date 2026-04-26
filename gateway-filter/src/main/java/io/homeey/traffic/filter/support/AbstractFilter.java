package io.homeey.traffic.filter.support;

import io.homeey.traffic.filter.core.FilterChain;
import io.homeey.traffic.filter.core.FilterContext;

public abstract class AbstractFilter implements OrderedFilter {

    @Override
    public final void filter(FilterContext context, FilterChain chain) throws Exception {
        if (shouldSkip(context)) {
            chain.filter(context);
            return;
        }
        doFilter(context, chain);
    }

    protected boolean shouldSkip(FilterContext context) {
        return false;
    }

    protected abstract void doFilter(FilterContext context, FilterChain chain) throws Exception;
}
