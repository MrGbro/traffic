package io.homeey.traffic.spi;

import io.homeey.traffic.spi.contract.transport.TransportRequestHandler;
import io.homeey.traffic.spi.contract.transport.TransportServer;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class TransportContractTest {

    @Test
    void transportServerShouldExposeStartAndStop() throws Exception {
        Method start = TransportServer.class.getMethod("start", int.class, TransportRequestHandler.class);
        Method stop = TransportServer.class.getMethod("stop");

        assertThat(start).isNotNull();
        assertThat(stop).isNotNull();
    }
}
