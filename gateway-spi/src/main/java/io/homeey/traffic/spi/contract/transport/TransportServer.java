package io.homeey.traffic.spi.contract.transport;

import io.homeey.traffic.spi.extension.SPI;

@SPI("jdkHttp")
public interface TransportServer {

    void start(int port, TransportRequestHandler handler) throws Exception;

    void stop();
}
