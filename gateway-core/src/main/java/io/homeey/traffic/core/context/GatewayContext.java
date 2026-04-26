package io.homeey.traffic.core.context;

import io.homeey.traffic.common.Phase;
import io.homeey.traffic.filter.core.FilterContext;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class GatewayContext implements FilterContext {

    private final String requestId;
    private Phase currentPhase;
    private final Map<String, Object> attributes = new ConcurrentHashMap<>();
    private final long startTime = System.currentTimeMillis();
    private volatile boolean terminated;
    private Integer statusCode;
    private String terminateReason;
    private Throwable error;

    public GatewayContext(String requestId) {
        this.requestId = requestId;
        this.currentPhase = Phase.PRE_ROUTE;
    }

    @Override
    public String requestId() {
        return requestId;
    }

    @Override
    public Phase currentPhase() {
        return currentPhase;
    }

    @Override
    public void currentPhase(Phase phase) {
        this.currentPhase = phase;
    }

    public long startTime() {
        return startTime;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T attribute(String key) {
        return (T) attributes.get(key);
    }

    @Override
    public void attribute(String key, Object value) {
        attributes.put(key, value);
    }

    public Map<String, Object> attributes() {
        return Map.copyOf(attributes);
    }

    @Override
    public boolean isTerminated() {
        return terminated;
    }

    @Override
    public void terminate(int statusCode, String reason) {
        this.terminated = true;
        this.statusCode = statusCode;
        this.terminateReason = reason;
    }

    public Optional<Integer> statusCode() {
        return Optional.ofNullable(statusCode);
    }

    public Optional<String> terminateReason() {
        return Optional.ofNullable(terminateReason);
    }

    @Override
    public boolean hasError() {
        return error != null;
    }

    @Override
    public Throwable error() {
        return error;
    }

    @Override
    public void error(Throwable error) {
        this.error = error;
    }
}
