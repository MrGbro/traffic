package io.homeey.traffic.spi.context;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public record SpiResponseContext(
        int statusCode,
        Map<String, String> headers,
        byte[] body
) {
    public SpiResponseContext {
        headers = Collections.unmodifiableMap(
                new HashMap<>(headers != null ? headers : Map.of())
        );
        body = body != null ? body.clone() : new byte[0];
    }
}
