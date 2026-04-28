package io.homeey.traffic.spi.contract.cluster;

import java.util.Map;

public record ServiceInstance(
        String serviceName,
        String host,
        int port,
        Map<String, String> metadata
) {
    public ServiceInstance {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public String toUrl() {
        return "http://" + host + ":" + port;
    }
}
