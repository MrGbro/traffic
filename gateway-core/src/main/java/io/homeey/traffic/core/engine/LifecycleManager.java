package io.homeey.traffic.core.engine;

import io.homeey.traffic.common.Phase;
import io.homeey.traffic.core.lifecycle.PhaseRegistry;

public class LifecycleManager {

    private final PhaseRegistry phaseRegistry;

    public LifecycleManager(PhaseRegistry phaseRegistry) {
        this.phaseRegistry = phaseRegistry;
    }

    public void onPhase(Phase phase, Runnable handler) {
        phaseRegistry.register(phase, handler);
    }

    public PhaseRegistry phaseRegistry() {
        return phaseRegistry;
    }
}
