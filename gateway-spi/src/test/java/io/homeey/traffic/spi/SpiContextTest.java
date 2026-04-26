package io.homeey.traffic.spi;

import io.homeey.traffic.spi.context.SpiRequestContext;
import io.homeey.traffic.spi.context.SpiResponseContext;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SpiContextTest {

    @Test
    void requestContextShouldBeImmutableAndCloneBody() {
        byte[] body = {1, 2, 3};
        var ctx = new SpiRequestContext(
                "req-1",
                "/api/test",
                "GET",
                Map.of("Content-Type", "application/json"),
                body,
                Map.of("traceId", "t1")
        );

        body[0] = 9;

        assertThat(ctx.requestId()).isEqualTo("req-1");
        assertThat(ctx.path()).isEqualTo("/api/test");
        assertThat(ctx.method()).isEqualTo("GET");
        assertThat(ctx.headers()).containsEntry("Content-Type", "application/json");
        assertThat(ctx.attributes()).containsEntry("traceId", "t1");
        assertThat(ctx.body()).containsExactly(1, 2, 3);
        assertThatThrownBy(() -> ctx.headers().put("k", "v")).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> ctx.attributes().put("k", "v")).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void responseContextShouldCloneBody() {
        byte[] body = {1, 2, 3};
        var ctx = new SpiResponseContext(200, Map.of("Content-Type", "text/plain"), body);

        body[0] = 9;

        assertThat(ctx.statusCode()).isEqualTo(200);
        assertThat(ctx.headers()).containsEntry("Content-Type", "text/plain");
        assertThat(ctx.body()).containsExactly(1, 2, 3);
    }
}
