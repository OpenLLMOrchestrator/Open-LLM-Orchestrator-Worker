package com.openllmorchestrator.worker.engine.bootstrap.steps;

import com.openllmorchestrator.worker.engine.bootstrap.BootstrapContext;
import com.openllmorchestrator.worker.engine.bootstrap.BootstrapStep;
import com.openllmorchestrator.worker.engine.stage.StagePlan;
import com.openllmorchestrator.worker.engine.stage.plan.StagePlanFactory;

/**
 * Step: build execution hierarchy (stage plan) once from config.
 * The plan is immutable, reused for the container lifecycle, and holds no transactional data.
 */
public final class BuildPlanStep implements BootstrapStep {
    @Override
    public void run(BootstrapContext ctx) {
        ctx.setPlan(StagePlanFactory.fromFileConfig(ctx.getConfig()));
    }
}
