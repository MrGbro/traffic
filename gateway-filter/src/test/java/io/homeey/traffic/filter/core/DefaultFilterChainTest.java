package io.homeey.traffic.filter.core;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultFilterChainTest {

    @Test
    void shouldInvokeFiltersInOrder() throws Exception {
        List<String> calls = new ArrayList<>();
        GatewayFilter f1 = (ctx, chain) -> {
            calls.add("f1");
            chain.filter(ctx);
        };
        GatewayFilter f2 = (ctx, chain) -> {
            calls.add("f2");
            chain.filter(ctx);
        };
        GatewayFilter f3 = (ctx, chain) -> calls.add("f3");

        DefaultFilterChain chain = new DefaultFilterChain(List.of(f1, f2, f3));
        chain.filter(new StubFilterContext());

        assertThat(calls).containsExactly("f1", "f2", "f3");
    }

    @Test
    void shouldShortCircuitWhenFilterDoesNotCallNext() throws Exception {
        List<String> calls = new ArrayList<>();
        GatewayFilter f1 = (ctx, chain) -> calls.add("f1");
        GatewayFilter f2 = (ctx, chain) -> calls.add("f2");

        DefaultFilterChain chain = new DefaultFilterChain(List.of(f1, f2));
        chain.filter(new StubFilterContext());

        assertThat(calls).containsExactly("f1");
    }
}
