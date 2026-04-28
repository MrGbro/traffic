package io.homeey.traffic.cluster.discovery;

import io.homeey.traffic.spi.contract.cluster.ServiceInstance;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryServiceDiscoveryTest {

    @Test
    void shouldReturnInstancesForServiceName() {
        ServiceInstance i1 = new ServiceInstance("orders", "127.0.0.1", 8081, Map.of());
        ServiceInstance i2 = new ServiceInstance("orders", "127.0.0.1", 8082, Map.of());

        InMemoryServiceDiscovery discovery = new InMemoryServiceDiscovery(
                Map.of("orders", List.of(i1, i2))
        );

        List<ServiceInstance> result = discovery.getInstances("orders");

        assertThat(result).containsExactly(i1, i2);
        assertThatThrownBy(() -> result.add(i1)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void shouldReturnEmptyWhenServiceMissing() {
        InMemoryServiceDiscovery discovery = new InMemoryServiceDiscovery(Map.of());

        assertThat(discovery.getInstances("orders")).isEmpty();
    }
}
