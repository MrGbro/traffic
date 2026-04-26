package io.homeey.traffic.spi.contract.cluster;

import io.homeey.traffic.spi.extension.SPI;

import java.util.List;

@SPI
public interface ServiceDiscovery {

    List<ServiceInstance> getInstances(String serviceName);
}
