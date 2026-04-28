package io.homeey.traffic.runtime.config;

import java.util.List;
import java.util.Map;

public record GatewayRuntimeConfig(
        int port,
        StaticConfig staticConfig,
        SpiConfig spiConfig,
        List<RouteConfig> routes,
        Map<String, List<String>> serviceInstances
) {
    public record StaticConfig(boolean enabled, String uriPrefix, String rootDir) {
    }

    public record SpiConfig(
            String transport,
            String forwarder,
            String serviceDiscovery,
            String loadBalancer,
            String routeLocator
    ) {
    }

    public record RouteConfig(String id, String target, int order, String path, String method) {
    }
}
