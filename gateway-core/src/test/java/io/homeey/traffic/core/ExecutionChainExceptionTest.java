package io.homeey.traffic.core;

import io.homeey.traffic.common.Phase;
import io.homeey.traffic.core.context.GatewayContext;
import io.homeey.traffic.core.engine.GatewayEngine;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExecutionChainExceptionTest {

    @Test
    void shouldEnterResponseWhenFilterThrowsException() {
        GatewayEngine engine = new GatewayEngine();
        List<Phase> visited = new ArrayList<>();

        engine.registerFilter(Phase.PRE_ROUTE, (ctx, chain) -> {
            visited.add(Phase.PRE_ROUTE);
            throw new IllegalStateException("boom");
        });
        engine.registerFilter(Phase.ROUTE, (ctx, chain) -> {
            visited.add(Phase.ROUTE);
            chain.filter(ctx);
        });
        engine.registerFilter(Phase.RESPONSE, (ctx, chain) -> visited.add(Phase.RESPONSE));

        GatewayContext context = new GatewayContext("req-1");
        engine.execute(context);

        assertThat(context.hasError()).isTrue();
        assertThat(context.isTerminated()).isTrue();
        assertThat(visited).containsExactly(
                Phase.PRE_ROUTE,
                Phase.RESPONSE
        );
    }
}
