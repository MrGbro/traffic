package io.homeey.traffic.cluster.discovery;

import io.homeey.traffic.spi.contract.cluster.ServiceDiscovery;
import io.homeey.traffic.spi.contract.cluster.ServiceInstance;

import java.util.List;
import java.util.Map;

public class InMemoryServiceDiscovery implements ServiceDiscovery {

    private final Map<String, List<ServiceInstance>> registry;

    public InMemoryServiceDiscovery(Map<String, List<ServiceInstance>> registry) {
        this.registry = registry == null ? Map.of() : Map.copyOf(registry);
    }

    @Override
    public List<ServiceInstance> getInstances(String serviceName) {
        List<ServiceInstance> instances = registry.get(serviceName);
        if (instances == null) {
            return List.of();
        }
        return List.copyOf(instances);
    }
}
