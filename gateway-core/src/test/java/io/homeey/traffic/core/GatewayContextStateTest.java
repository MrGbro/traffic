package io.homeey.traffic.core;

import io.homeey.traffic.core.context.GatewayContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayContextStateTest {

    @Test
    void shouldMarkTerminatedWithStatusAndReason() {
        GatewayContext ctx = new GatewayContext("req-1");
        ctx.terminate(403, "forbidden");

        assertThat(ctx.isTerminated()).isTrue();
        assertThat(ctx.statusCode()).contains(403);
        assertThat(ctx.terminateReason()).contains("forbidden");
    }

    @Test
    void shouldRecordError() {
        GatewayContext ctx = new GatewayContext("req-1");
        RuntimeException ex = new RuntimeException("boom");
        ctx.error(ex);

        assertThat(ctx.hasError()).isTrue();
        assertThat(ctx.error()).isSameAs(ex);
    }
}
