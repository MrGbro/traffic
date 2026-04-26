package io.homeey.traffic.core.context;

import io.homeey.traffic.common.Phase;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GatewayContext {

    private final String requestId;
    private Phase currentPhase;
    private final Map<String, Object> attributes = new ConcurrentHashMap<>();
    private final long startTime = System.currentTimeMillis();

    public GatewayContext(String requestId) {
        this.requestId = requestId;
        this.currentPhase = Phase.PRE_ROUTE;
    }

    public String requestId() {
        return requestId;
    }

    public Phase currentPhase() {
        return currentPhase;
    }

    public void currentPhase(Phase phase) {
        this.currentPhase = phase;
    }

    public long startTime() {
        return startTime;
    }

    @SuppressWarnings("unchecked")
    public <T> T attribute(String key) {
        return (T) attributes.get(key);
    }

    public void attribute(String key, Object value) {
        attributes.put(key, value);
    }

    public Map<String, Object> attributes() {
        return Map.copyOf(attributes);
    }
}
