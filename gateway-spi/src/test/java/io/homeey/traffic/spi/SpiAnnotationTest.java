package io.homeey.traffic.spi;

import io.homeey.traffic.spi.extension.Activate;
import io.homeey.traffic.spi.extension.SPI;
import io.homeey.traffic.spi.contract.cluster.LoadBalancer;
import io.homeey.traffic.spi.contract.cluster.ServiceDiscovery;
import io.homeey.traffic.spi.contract.forward.Forwarder;
import io.homeey.traffic.spi.contract.transport.TransportServer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SpiAnnotationTest {

    @Test
    void spiAnnotationShouldBeRuntimeRetention() {
        var retention = SPI.class.getAnnotation(java.lang.annotation.Retention.class);
        assertThat(retention).isNotNull();
        assertThat(retention.value()).isEqualTo(java.lang.annotation.RetentionPolicy.RUNTIME);
    }

    @Test
    void activateAnnotationShouldHaveDefaults() {
        Activate activate = DefaultImpl.class.getAnnotation(Activate.class);
        assertThat(activate.group()).isEmpty();
        assertThat(activate.order()).isEqualTo(0);
    }

    @Test
    void spiContractsShouldDeclareDefaultName() {
        assertThat(Forwarder.class.getAnnotation(SPI.class).value()).isEqualTo("http");
        assertThat(ServiceDiscovery.class.getAnnotation(SPI.class).value()).isEqualTo("inMemory");
        assertThat(LoadBalancer.class.getAnnotation(SPI.class).value()).isEqualTo("roundRobin");
        assertThat(TransportServer.class.getAnnotation(SPI.class).value()).isEqualTo("jdkHttp");
    }

    @Activate
    static class DefaultImpl {}
}
