package io.homeey.traffic.core.engine;

import io.homeey.traffic.common.Phase;
import io.homeey.traffic.core.lifecycle.PhaseRegistry;
import io.homeey.traffic.filter.core.GatewayFilter;
import io.homeey.traffic.spi.extension.Activate;

public class LifecycleManager {

    private final PhaseRegistry phaseRegistry;

    public LifecycleManager(PhaseRegistry phaseRegistry) {
        this.phaseRegistry = phaseRegistry;
    }

    public LifecycleManager registerFilter(Phase phase, GatewayFilter filter) {
        phaseRegistry.register(phase, filter);
        return this;
    }

    public LifecycleManager registerFilter(Phase phase, GatewayFilter filter, Activate activate) {
        phaseRegistry.register(phase, filter, activate);
        return this;
    }

    public PhaseRegistry phaseRegistry() {
        return phaseRegistry;
    }
}
