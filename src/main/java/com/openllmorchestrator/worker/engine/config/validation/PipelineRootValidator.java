package com.openllmorchestrator.worker.engine.config.validation;

import com.openllmorchestrator.worker.engine.config.EngineFileConfig;
import com.openllmorchestrator.worker.engine.stage.resolver.StageResolver;

/** Validates pipeline section: either root (legacy) or stages (top-level flow) must be set. */
public final class PipelineRootValidator implements ConfigValidator {
    @Override
    public void validate(EngineFileConfig config, StageResolver resolver) {
        if (config.getPipeline() == null) {
            throw new IllegalStateException("config.pipeline is required");
        }
        boolean hasStages = config.getPipeline().getStages() != null && !config.getPipeline().getStages().isEmpty();
        boolean hasRoot = config.getPipeline().getRoot() != null;
        if (!hasStages && !hasRoot) {
            throw new IllegalStateException("config.pipeline must have either pipeline.root or pipeline.stages");
        }
        if (config.getPipeline().getDefaultTimeoutSeconds() <= 0) {
            throw new IllegalStateException("config.pipeline.defaultTimeoutSeconds must be positive");
        }
    }
}
