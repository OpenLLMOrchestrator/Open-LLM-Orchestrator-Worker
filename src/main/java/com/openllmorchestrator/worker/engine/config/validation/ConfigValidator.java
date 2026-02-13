package com.openllmorchestrator.worker.engine.config.validation;

import com.openllmorchestrator.worker.engine.config.EngineFileConfig;
import com.openllmorchestrator.worker.engine.stage.resolver.StageResolver;

/** One validation concern. Add new validators without changing existing ones (OCP). */
public interface ConfigValidator {
    void validate(EngineFileConfig config, StageResolver resolver);
}
