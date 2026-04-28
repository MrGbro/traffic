package io.homeey.traffic.runtime.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayRuntimeConfigLoaderTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldLoadGatewayRuntimeConfig() throws Exception {
        Path file = tempDir.resolve("gateway.properties");
        Files.writeString(file, """
                gateway.server.port=18080
                gateway.static.enabled=true
                gateway.static.uriPrefix=/assets/
                gateway.static.rootDir=./public
                gateway.spi.transport=jdkHttp
                gateway.spi.forwarder=http
                gateway.spi.serviceDiscovery=inMemory
                gateway.spi.loadBalancer=roundRobin
                gateway.spi.routeLocator=inMemory
                gateway.routes=orders
                gateway.route.orders.target=svc://orders
                gateway.route.orders.order=10
                gateway.route.orders.path=/orders
                gateway.route.orders.method=GET
                gateway.services=orders
                gateway.service.orders.instances=127.0.0.1:9001,127.0.0.1:9002
                """);

        GatewayRuntimeConfig config = GatewayRuntimeConfigLoader.load(file);

        assertThat(config.port()).isEqualTo(18080);
        assertThat(config.staticConfig().enabled()).isTrue();
        assertThat(config.staticConfig().uriPrefix()).isEqualTo("/assets/");
        assertThat(config.staticConfig().rootDir()).isEqualTo("./public");
        assertThat(config.spiConfig().transport()).isEqualTo("jdkHttp");
        assertThat(config.spiConfig().forwarder()).isEqualTo("http");
        assertThat(config.spiConfig().serviceDiscovery()).isEqualTo("inMemory");
        assertThat(config.spiConfig().loadBalancer()).isEqualTo("roundRobin");
        assertThat(config.spiConfig().routeLocator()).isEqualTo("inMemory");
        assertThat(config.routes()).hasSize(1);
        assertThat(config.routes().get(0).target()).isEqualTo("svc://orders");
        assertThat(config.serviceInstances().get("orders")).containsExactly("127.0.0.1:9001", "127.0.0.1:9002");
    }

    @Test
    void shouldKeepSpiConfigFieldsNullWhenNotConfigured() throws Exception {
        Path file = tempDir.resolve("gateway-default.properties");
        Files.writeString(file, """
                gateway.server.port=8080
                """);

        GatewayRuntimeConfig config = GatewayRuntimeConfigLoader.load(file);

        assertThat(config.spiConfig().transport()).isNull();
        assertThat(config.spiConfig().forwarder()).isNull();
        assertThat(config.spiConfig().serviceDiscovery()).isNull();
        assertThat(config.spiConfig().loadBalancer()).isNull();
        assertThat(config.spiConfig().routeLocator()).isNull();
    }
}
