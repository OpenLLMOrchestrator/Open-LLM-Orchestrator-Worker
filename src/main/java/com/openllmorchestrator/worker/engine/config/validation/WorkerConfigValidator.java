package com.openllmorchestrator.worker.engine.config.validation;

import com.openllmorchestrator.worker.engine.config.EngineFileConfig;
import com.openllmorchestrator.worker.engine.stage.resolver.StageResolver;

/** Validates worker section. */
public final class WorkerConfigValidator implements ConfigValidator {
    @Override
    public void validate(EngineFileConfig config, StageResolver resolver) {
        if (config.getWorker() == null || config.getWorker().getQueueName() == null
                || config.getWorker().getQueueName().isBlank()) {
            throw new IllegalStateException("config.worker.queueName is required");
        }
    }
}
