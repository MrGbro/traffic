package io.homeey.traffic.runtime.extension;

import io.homeey.traffic.spi.extension.SPI;

@SPI("spiDefault")
public interface ResolverOrderTestSpi {

    String name();
}
