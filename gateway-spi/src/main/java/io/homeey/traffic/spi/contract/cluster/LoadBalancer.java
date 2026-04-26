package io.homeey.traffic.spi.contract.cluster;

import io.homeey.traffic.spi.extension.SPI;

import java.util.List;
import java.util.Optional;

@SPI
public interface LoadBalancer {

    Optional<ServiceInstance> choose(String serviceName, List<ServiceInstance> instances);
}
