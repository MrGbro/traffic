package io.homeey.traffic.spi.context;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public record SpiRequestContext(
        String requestId,
        String path,
        String method,
        Map<String, String> headers,
        byte[] body,
        Map<String, String> attributes
) {
    public SpiRequestContext {
        headers = Collections.unmodifiableMap(new HashMap<>(headers != null ? headers : Map.of()));
        body = body != null ? body.clone() : new byte[0];
        attributes = Collections.unmodifiableMap(new HashMap<>(attributes != null ? attributes : Map.of()));
    }
}
