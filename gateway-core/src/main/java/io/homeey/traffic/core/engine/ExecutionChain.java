package io.homeey.traffic.core.engine;

import io.homeey.traffic.common.Phase;
import io.homeey.traffic.core.context.GatewayContext;
import io.homeey.traffic.core.lifecycle.PhaseRegistry;
import io.homeey.traffic.filter.core.DefaultFilterChain;
import io.homeey.traffic.filter.core.GatewayFilter;

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
            if (context.isTerminated() && phase != Phase.RESPONSE) {
                continue;
            }
            context.currentPhase(phase);
            List<GatewayFilter> filters = phaseRegistry.filters(phase);
            try {
                new DefaultFilterChain(filters).filter(context);
            } catch (Exception e) {
                context.error(e);
                if (!context.isTerminated()) {
                    context.terminate(500, e.getMessage() != null ? e.getMessage() : "Internal error");
                }
            }
        }
    }
}
