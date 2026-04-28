package io.homeey.traffic.runtime.bootstrap;

import io.homeey.traffic.common.Phase;
import io.homeey.traffic.core.context.GatewayContext;
import io.homeey.traffic.core.engine.GatewayEngine;
import io.homeey.traffic.core.engine.forward.ForwardPhaseFilter;
import io.homeey.traffic.core.engine.route.RoutePhaseFilter;
import io.homeey.traffic.routing.locator.RouteLocator;
import io.homeey.traffic.spi.context.ExchangeAttributes;
import io.homeey.traffic.spi.context.SpiRequestContext;
import io.homeey.traffic.spi.context.SpiResponseContext;
import io.homeey.traffic.spi.contract.cluster.LoadBalancer;
import io.homeey.traffic.spi.contract.cluster.ServiceDiscovery;
import io.homeey.traffic.spi.contract.forward.Forwarder;
import io.homeey.traffic.spi.contract.transport.TransportServer;

import java.util.Map;

public class GatewayBootstrap {

    private final GatewayEngine engine;
    private final TransportServer transportServer;

    public GatewayBootstrap(RouteLocator routeLocator,
                            ServiceDiscovery serviceDiscovery,
                            LoadBalancer loadBalancer,
                            Forwarder forwarder,
                            TransportServer transportServer) {
        this.transportServer = transportServer;
        this.engine = new GatewayEngine();

        engine.registerFilter(Phase.ROUTE, new RoutePhaseFilter(routeLocator));
        engine.registerFilter(Phase.FORWARD, new ForwardPhaseFilter(forwarder, serviceDiscovery, loadBalancer));
    }

    public void start(int port) throws Exception {
        transportServer.start(port, this::handle);
    }

    public void stop() {
        transportServer.stop();
    }

    private SpiResponseContext handle(SpiRequestContext request) {
        GatewayContext context = new GatewayContext(request.requestId());
        context.attribute(ExchangeAttributes.REQUEST_PATH, request.path());
        context.attribute(ExchangeAttributes.REQUEST_METHOD, request.method());
        context.attribute(ExchangeAttributes.REQUEST_HEADERS, request.headers());
        context.attribute(ExchangeAttributes.REQUEST_BODY, request.body());

        engine.execute(context);

        Integer status = context.attribute(ExchangeAttributes.RESPONSE_STATUS);
        @SuppressWarnings("unchecked")
        Map<String, String> headers = context.attribute(ExchangeAttributes.RESPONSE_HEADERS);
        byte[] body = context.attribute(ExchangeAttributes.RESPONSE_BODY);

        if (status != null) {
            return new SpiResponseContext(status, headers == null ? Map.of() : headers, body == null ? new byte[0] : body);
        }

        if (context.statusCode().isPresent()) {
            return new SpiResponseContext(
                    context.statusCode().get(),
                    Map.of("Content-Type", "text/plain"),
                    context.terminateReason().orElse("").getBytes()
            );
        }

        return new SpiResponseContext(200, Map.of(), new byte[0]);
    }
}
