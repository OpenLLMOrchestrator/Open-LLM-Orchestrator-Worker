package com.openllmorchestrator.worker.engine.config.validation;

import com.openllmorchestrator.worker.engine.config.EngineFileConfig;
import com.openllmorchestrator.worker.engine.stage.resolver.StageResolver;

/** Validates config root is non-null. */
public final class NotNullConfigValidator implements ConfigValidator {
    @Override
    public void validate(EngineFileConfig config, StageResolver resolver) {
        if (config == null) {
            throw new IllegalStateException("Engine config is null");
        }
    }
}
