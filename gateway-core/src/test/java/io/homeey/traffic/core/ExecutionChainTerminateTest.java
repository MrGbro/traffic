package io.homeey.traffic.core;

import io.homeey.traffic.common.Phase;
import io.homeey.traffic.core.context.GatewayContext;
import io.homeey.traffic.core.engine.GatewayEngine;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExecutionChainTerminateTest {

    @Test
    void shouldSkipMiddlePhasesButStillRunResponseWhenTerminated() {
        GatewayEngine engine = new GatewayEngine();
        List<Phase> visited = new ArrayList<>();

        engine.registerFilter(Phase.PRE_ROUTE, (ctx, chain) -> {
            visited.add(Phase.PRE_ROUTE);
            chain.filter(ctx);
        });
        engine.registerFilter(Phase.ROUTE, (ctx, chain) -> {
            visited.add(Phase.ROUTE);
            ctx.terminate(403, "forbidden");
        });
        engine.registerFilter(Phase.PRE_FORWARD, (ctx, chain) -> {
            visited.add(Phase.PRE_FORWARD);
            chain.filter(ctx);
        });
        engine.registerFilter(Phase.RESPONSE, (ctx, chain) -> visited.add(Phase.RESPONSE));

        engine.execute(new GatewayContext("req-1"));

        assertThat(visited).containsExactly(
                Phase.PRE_ROUTE,
                Phase.ROUTE,
                Phase.RESPONSE
        );
    }
}
