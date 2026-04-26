package io.homeey.traffic.spi.context;

public record SpiRequestContext(
        String requestId,
        String path,
        String method,
        java.util.Map<String, String> attributes
) {
    public SpiRequestContext {
        attributes = java.util.Collections.unmodifiableMap(
                new java.util.HashMap<>(attributes != null ? attributes : java.util.Map.of())
        );
    }
}
