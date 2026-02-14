package com.openllmorchestrator.worker.engine.bootstrap.steps;

import com.openllmorchestrator.worker.engine.bootstrap.BootstrapContext;
import com.openllmorchestrator.worker.engine.bootstrap.BootstrapStep;
import com.openllmorchestrator.worker.engine.runtime.EngineRuntime;

/** Step: set resolver, config, and execution hierarchy (plan) on runtime for container lifecycle. */
public final class SetRuntimeStep implements BootstrapStep {
    @Override
    public void run(BootstrapContext ctx) {
        EngineRuntime.setStageResolver(ctx.getResolver());
        EngineRuntime.setConfig(ctx.getConfig());
        EngineRuntime.setStagePlans(ctx.getPlans());
        EngineRuntime.CONFIG = ctx.getConfig();
    }
}
