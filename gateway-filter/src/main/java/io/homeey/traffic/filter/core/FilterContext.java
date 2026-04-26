package io.homeey.traffic.filter.core;

import io.homeey.traffic.common.Phase;

public interface FilterContext {

    String requestId();

    Phase currentPhase();

    void currentPhase(Phase phase);

    <T> T attribute(String key);

    void attribute(String key, Object value);

    boolean isTerminated();

    void terminate(int statusCode, String reason);

    boolean hasError();

    Throwable error();

    void error(Throwable error);
}
