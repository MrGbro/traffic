package io.homeey.traffic.cluster.loadbalance;

import io.homeey.traffic.spi.contract.cluster.LoadBalancer;
import io.homeey.traffic.spi.contract.cluster.ServiceInstance;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class RoundRobinLoadBalancer implements LoadBalancer {

    private final ConcurrentHashMap<String, AtomicInteger> counters = new ConcurrentHashMap<>();

    @Override
    public Optional<ServiceInstance> choose(String serviceName, List<ServiceInstance> instances) {
        if (instances == null || instances.isEmpty()) {
            return Optional.empty();
        }
        AtomicInteger counter = counters.computeIfAbsent(serviceName, key -> new AtomicInteger(0));
        int index = Math.floorMod(counter.getAndIncrement(), instances.size());
        return Optional.of(instances.get(index));
    }
}
