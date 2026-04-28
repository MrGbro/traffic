package io.homeey.traffic.spi.contract.cluster;

import io.homeey.traffic.spi.extension.SPI;

import java.util.List;

@SPI("inMemory")
public interface ServiceDiscovery {

    List<ServiceInstance> getInstances(String serviceName);
}
