package io.homeey.traffic.spi;

import io.homeey.traffic.spi.extension.Activate;
import io.homeey.traffic.spi.extension.SPI;
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

    @Activate
    static class DefaultImpl {}
}
