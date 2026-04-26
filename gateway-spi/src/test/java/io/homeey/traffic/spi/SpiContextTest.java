package io.homeey.traffic.spi;

import io.homeey.traffic.spi.context.SpiRequestContext;
import io.homeey.traffic.spi.context.SpiResponseContext;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SpiContextTest {

    @Test
    void requestContextShouldBeImmutable() {
        var ctx = new SpiRequestContext("req-1", "/api/test", "GET", Map.of("key", "value"));
        assertThat(ctx.requestId()).isEqualTo("req-1");
        assertThat(ctx.path()).isEqualTo("/api/test");
        assertThat(ctx.method()).isEqualTo("GET");
        assertThat(ctx.attributes()).containsEntry("key", "value");
    }

    @Test
    void responseContextShouldCloneBody() {
        byte[] body = {1, 2, 3};
        var ctx = new SpiResponseContext(200, Map.of("Content-Type", "text/plain"), body);
        assertThat(ctx.statusCode()).isEqualTo(200);
        assertThat(ctx.headers()).containsEntry("Content-Type", "text/plain");
        assertThat(ctx.body()).containsExactly(1, 2, 3);
    }
}
