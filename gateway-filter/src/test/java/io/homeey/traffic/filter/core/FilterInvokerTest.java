package io.homeey.traffic.filter.core;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class FilterInvokerTest {

    @Test
    void shouldInvokeFilter() throws Exception {
        AtomicBoolean called = new AtomicBoolean(false);
        GatewayFilter filter = (ctx, chain) -> called.set(true);
        FilterInvoker invoker = new FilterInvoker();

        invoker.invoke(filter, new StubFilterContext(), ctx -> {
        });

        assertThat(called).isTrue();
    }
}
