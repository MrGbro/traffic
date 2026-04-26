package io.homeey.traffic.core.engine;

import io.homeey.traffic.common.Phase;
import io.homeey.traffic.core.context.GatewayContext;
import io.homeey.traffic.core.lifecycle.PhaseRegistry;

import java.util.ArrayList;
import java.util.List;

public class ExecutionChain {

    private final PhaseRegistry phaseRegistry;
    private final List<Phase> orderedPhases;

    public ExecutionChain(PhaseRegistry phaseRegistry) {
        this.phaseRegistry = phaseRegistry;
        this.orderedPhases = List.of(Phase.values());
    }

    public void execute(GatewayContext context) {
        for (Phase phase : orderedPhases) {
            context.currentPhase(phase);
            phaseRegistry.handlers(phase).forEach(Runnable::run);
        }
    }
}
