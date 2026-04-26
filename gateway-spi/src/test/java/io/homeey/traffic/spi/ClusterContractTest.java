package io.homeey.traffic.spi;

import io.homeey.traffic.spi.contract.cluster.ServiceInstance;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClusterContractTest {

    @Test
    void serviceInstanceShouldExposeUrlAndImmutableMetadata() {
        ServiceInstance instance = new ServiceInstance("orders", "127.0.0.1", 8081, Map.of("zone", "a"));

        assertThat(instance.toUrl()).isEqualTo("http://127.0.0.1:8081");
        assertThat(instance.metadata()).containsEntry("zone", "a");
        assertThatThrownBy(() -> instance.metadata().put("zone", "b"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
