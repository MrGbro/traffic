package io.homeey.traffic.core;

import io.homeey.traffic.common.Phase;
import io.homeey.traffic.core.context.GatewayContext;
import io.homeey.traffic.core.engine.GatewayEngine;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayEngineTest {

    @Test
    void shouldExecuteThroughAllPhasesInOrder() {
        GatewayEngine engine = new GatewayEngine();
        List<Phase> visited = new ArrayList<>();

        engine.lifecycleManager().onPhase(Phase.PRE_ROUTE, () -> visited.add(Phase.PRE_ROUTE));
        engine.lifecycleManager().onPhase(Phase.ROUTE, () -> visited.add(Phase.ROUTE));
        engine.lifecycleManager().onPhase(Phase.PRE_FORWARD, () -> visited.add(Phase.PRE_FORWARD));
        engine.lifecycleManager().onPhase(Phase.FORWARD, () -> visited.add(Phase.FORWARD));
        engine.lifecycleManager().onPhase(Phase.POST_FORWARD, () -> visited.add(Phase.POST_FORWARD));
        engine.lifecycleManager().onPhase(Phase.RESPONSE, () -> visited.add(Phase.RESPONSE));

        GatewayContext ctx = new GatewayContext("req-1");
        engine.execute(ctx);

        assertThat(visited).containsExactly(
                Phase.PRE_ROUTE,
                Phase.ROUTE,
                Phase.PRE_FORWARD,
                Phase.FORWARD,
                Phase.POST_FORWARD,
                Phase.RESPONSE
        );
    }

    @Test
    void contextShouldTrackCurrentPhase() {
        GatewayEngine engine = new GatewayEngine();
        List<Phase> phases = new ArrayList<>();

        engine.lifecycleManager().onPhase(Phase.PRE_ROUTE, () -> {});
        engine.lifecycleManager().onPhase(Phase.ROUTE, () -> phases.add(Phase.ROUTE));

        GatewayContext ctx = new GatewayContext("req-1");
        engine.execute(ctx);

        assertThat(phases).contains(Phase.ROUTE);
    }
}
