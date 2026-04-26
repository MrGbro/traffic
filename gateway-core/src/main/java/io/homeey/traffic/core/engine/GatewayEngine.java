package io.homeey.traffic.core.engine;

import io.homeey.traffic.common.Phase;
import io.homeey.traffic.core.context.GatewayContext;
import io.homeey.traffic.core.lifecycle.PhaseRegistry;
import io.homeey.traffic.filter.core.GatewayFilter;
import io.homeey.traffic.spi.extension.Activate;

public class GatewayEngine {

    private final ExecutionChain executionChain;
    private final LifecycleManager lifecycleManager;

    public GatewayEngine() {
        PhaseRegistry phaseRegistry = new PhaseRegistry();
        this.lifecycleManager = new LifecycleManager(phaseRegistry);
        this.executionChain = new ExecutionChain(phaseRegistry);
    }

    public void execute(GatewayContext context) {
        executionChain.execute(context);
    }

    public GatewayEngine registerFilter(Phase phase, GatewayFilter filter) {
        lifecycleManager.registerFilter(phase, filter);
        return this;
    }

    public GatewayEngine registerFilter(Phase phase, GatewayFilter filter, Activate activate) {
        lifecycleManager.registerFilter(phase, filter, activate);
        return this;
    }

    public LifecycleManager lifecycleManager() {
        return lifecycleManager;
    }
}
