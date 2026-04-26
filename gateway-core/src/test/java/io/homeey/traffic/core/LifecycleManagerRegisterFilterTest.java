package io.homeey.traffic.core;

import io.homeey.traffic.common.Phase;
import io.homeey.traffic.core.context.GatewayContext;
import io.homeey.traffic.core.engine.GatewayEngine;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class LifecycleManagerRegisterFilterTest {

    @Test
    void shouldRegisterFilterIntoTargetPhase() {
        GatewayEngine engine = new GatewayEngine();
        AtomicBoolean called = new AtomicBoolean(false);

        engine.lifecycleManager().registerFilter(Phase.ROUTE, (ctx, chain) -> {
            called.set(true);
            chain.filter(ctx);
        });

        engine.execute(new GatewayContext("req-1"));
        assertThat(called).isTrue();
    }
}
