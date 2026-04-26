package io.homeey.traffic.core;

import io.homeey.traffic.core.context.GatewayContext;
import io.homeey.traffic.core.engine.forward.ForwardPhaseFilter;
import io.homeey.traffic.filter.core.FilterChain;
import io.homeey.traffic.routing.context.RoutingAttributes;
import io.homeey.traffic.spi.context.ExchangeAttributes;
import io.homeey.traffic.spi.context.SpiResponseContext;
import io.homeey.traffic.spi.contract.forward.Forwarder;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class ForwardPhaseFilterTest {

    @Test
    void shouldWriteResponseAndContinueWhenForwardSuccess() throws Exception {
        Forwarder forwarder = (target, request) -> new SpiResponseContext(
                200,
                Map.of("Content-Type", "text/plain"),
                "ok".getBytes(StandardCharsets.UTF_8)
        );
        ForwardPhaseFilter filter = new ForwardPhaseFilter(forwarder);

        GatewayContext context = baseContext();
        AtomicBoolean continued = new AtomicBoolean(false);
        FilterChain chain = ctx -> continued.set(true);

        filter.filter(context, chain);

        Integer status = context.attribute(ExchangeAttributes.RESPONSE_STATUS);
        @SuppressWarnings("unchecked")
        Map<String, String> headers = context.attribute(ExchangeAttributes.RESPONSE_HEADERS);
        byte[] body = context.attribute(ExchangeAttributes.RESPONSE_BODY);

        assertThat(context.isTerminated()).isFalse();
        assertThat(status).isEqualTo(200);
        assertThat(headers).containsEntry("Content-Type", "text/plain");
        assertThat(new String(body, StandardCharsets.UTF_8)).isEqualTo("ok");
        assertThat(continued).isTrue();
    }

    @Test
    void shouldTerminate502WhenRouteTargetMissing() throws Exception {
        Forwarder forwarder = (target, request) -> new SpiResponseContext(200, Map.of(), new byte[0]);
        ForwardPhaseFilter filter = new ForwardPhaseFilter(forwarder);

        GatewayContext context = new GatewayContext("req-1");
        context.attribute(ExchangeAttributes.REQUEST_PATH, "/orders");
        context.attribute(ExchangeAttributes.REQUEST_METHOD, "GET");

        filter.filter(context, ctx -> {
        });

        assertThat(context.isTerminated()).isTrue();
        assertThat(context.statusCode()).contains(502);
        assertThat(context.terminateReason()).contains("Bad Gateway");
    }

    @Test
    void shouldMapTimeoutTo504() throws Exception {
        Forwarder forwarder = (target, request) -> {
            throw new HttpTimeoutException("timeout");
        };
        ForwardPhaseFilter filter = new ForwardPhaseFilter(forwarder);

        GatewayContext context = baseContext();

        filter.filter(context, ctx -> {
        });

        assertThat(context.isTerminated()).isTrue();
        assertThat(context.statusCode()).contains(504);
        assertThat(context.terminateReason()).contains("Gateway Timeout");
    }

    @Test
    void shouldMapIoExceptionTo502() throws Exception {
        Forwarder forwarder = (target, request) -> {
            throw new IOException("connect fail");
        };
        ForwardPhaseFilter filter = new ForwardPhaseFilter(forwarder);

        GatewayContext context = baseContext();

        filter.filter(context, ctx -> {
        });

        assertThat(context.isTerminated()).isTrue();
        assertThat(context.statusCode()).contains(502);
        assertThat(context.terminateReason()).contains("Bad Gateway");
    }

    private GatewayContext baseContext() {
        GatewayContext context = new GatewayContext("req-1");
        context.attribute(RoutingAttributes.ROUTE_TARGET, "http://localhost:8081");
        context.attribute(RoutingAttributes.ROUTE_ID, "r-orders");
        context.attribute(ExchangeAttributes.REQUEST_PATH, "/orders");
        context.attribute(ExchangeAttributes.REQUEST_METHOD, "GET");
        context.attribute(ExchangeAttributes.REQUEST_HEADERS, Map.of("Accept", "*/*"));
        context.attribute(ExchangeAttributes.REQUEST_BODY, new byte[0]);
        return context;
    }
}
