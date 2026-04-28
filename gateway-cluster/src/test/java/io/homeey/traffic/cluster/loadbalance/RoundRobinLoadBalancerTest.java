package io.homeey.traffic.cluster.loadbalance;

import io.homeey.traffic.spi.contract.cluster.ServiceInstance;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RoundRobinLoadBalancerTest {

    @Test
    void shouldChooseInstancesInRoundRobinOrder() {
        RoundRobinLoadBalancer lb = new RoundRobinLoadBalancer();
        ServiceInstance i1 = new ServiceInstance("orders", "127.0.0.1", 8081, Map.of());
        ServiceInstance i2 = new ServiceInstance("orders", "127.0.0.1", 8082, Map.of());

        List<ServiceInstance> instances = List.of(i1, i2);

        assertThat(lb.choose("orders", instances)).contains(i1);
        assertThat(lb.choose("orders", instances)).contains(i2);
        assertThat(lb.choose("orders", instances)).contains(i1);
    }

    @Test
    void shouldReturnEmptyWhenNoInstances() {
        RoundRobinLoadBalancer lb = new RoundRobinLoadBalancer();

        assertThat(lb.choose("orders", List.of())).isEmpty();
    }
}
