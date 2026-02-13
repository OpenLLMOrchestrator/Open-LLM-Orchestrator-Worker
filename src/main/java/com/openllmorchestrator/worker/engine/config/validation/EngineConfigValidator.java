package com.openllmorchestrator.worker.engine.config.validation;

import com.openllmorchestrator.worker.engine.config.EngineFileConfig;
import com.openllmorchestrator.worker.engine.stage.resolver.StageResolver;

import java.util.List;

/** Runs all config validators. Single responsibility: orchestrate validation. */
public final class EngineConfigValidator {
    private static final List<ConfigValidator> VALIDATORS = List.of(
            new NotNullConfigValidator(),
            new WorkerConfigValidator(),
            new PipelineRootValidator(),
            new PipelineNodeValidator(),
            new PipelineStagesValidator()
    );

    private EngineConfigValidator() {}

    public static void validate(EngineFileConfig config, StageResolver stageResolver) {
        for (ConfigValidator v : VALIDATORS) {
            v.validate(config, stageResolver);
        }
    }
}
