package io.homeey.traffic.spi.loader;

import io.homeey.traffic.spi.extension.SPI;

@SPI("first")
public interface NamedTestSpi {

    String name();
}
