package io.homeey.traffic.core.engine;

import io.homeey.traffic.core.context.GatewayContext;
import io.homeey.traffic.core.lifecycle.PhaseRegistry;

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

    public LifecycleManager lifecycleManager() {
        return lifecycleManager;
    }
}
