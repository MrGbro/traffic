package io.homeey.traffic.spi.contract.forward;

import io.homeey.traffic.spi.context.SpiRequestContext;
import io.homeey.traffic.spi.context.SpiResponseContext;
import io.homeey.traffic.spi.extension.SPI;

@SPI
public interface Forwarder {

    SpiResponseContext forward(String target, SpiRequestContext request) throws Exception;
}
