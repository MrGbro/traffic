package io.homeey.traffic.spi.contract.transport;

import io.homeey.traffic.spi.context.SpiRequestContext;
import io.homeey.traffic.spi.context.SpiResponseContext;

@FunctionalInterface
public interface TransportRequestHandler {

    SpiResponseContext handle(SpiRequestContext request);
}
