package io.homeey.traffic.filter.core;

import io.homeey.traffic.common.Phase;

import java.util.HashMap;
import java.util.Map;

public final class StubFilterContext implements FilterContext {

    private final Map<String, Object> attributes = new HashMap<>();
    private Phase phase = Phase.PRE_ROUTE;
    private boolean terminated;
    private Throwable error;

    @Override
    public String requestId() {
        return "stub";
    }

    @Override
    public Phase currentPhase() {
        return phase;
    }

    @Override
    public void currentPhase(Phase phase) {
        this.phase = phase;
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

    @Override
    public boolean isTerminated() {
        return terminated;
    }

    @Override
    public void terminate(int statusCode, String reason) {
        this.terminated = true;
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
