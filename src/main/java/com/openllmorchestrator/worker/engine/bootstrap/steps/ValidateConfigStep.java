package com.openllmorchestrator.worker.engine.bootstrap.steps;

import com.openllmorchestrator.worker.engine.bootstrap.BootstrapContext;
import com.openllmorchestrator.worker.engine.bootstrap.BootstrapStep;
import com.openllmorchestrator.worker.engine.config.validation.EngineConfigValidator;

/** Step: validate config and pipeline. */
public final class ValidateConfigStep implements BootstrapStep {
    @Override
    public void run(BootstrapContext ctx) {
        EngineConfigValidator.validate(ctx.getConfig(), ctx.getResolver());
    }
}
