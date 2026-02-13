package com.openllmorchestrator.worker.engine.config.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openllmorchestrator.worker.engine.config.EngineFileConfig;
import com.openllmorchestrator.worker.engine.config.pipeline.GroupConfig;
import com.openllmorchestrator.worker.engine.config.pipeline.StageBlockConfig;
import com.openllmorchestrator.worker.engine.stage.resolver.StageResolver;

import java.util.List;
import java.util.Map;

/** Validates pipeline.stages: stage names, group executionMode, and that every activity name is resolvable. */
public final class PipelineStagesValidator implements ConfigValidator {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public void validate(EngineFileConfig config, StageResolver resolver) {
        if (config.getPipeline() == null) {
            return;
        }
        List<StageBlockConfig> stages = config.getPipeline().getStages();
        if (stages == null || stages.isEmpty()) {
            return;
        }
        for (StageBlockConfig block : stages) {
            if (block == null) {
                throw new IllegalStateException("pipeline.stages contains a null stage block");
            }
            if (block.getStage() == null || block.getStage().isBlank()) {
                throw new IllegalStateException("pipeline.stages: each stage block must have a non-blank 'stage' name");
            }
            for (GroupConfig group : block.getGroupsSafe()) {
                validateGroup(group, resolver);
            }
        }
    }

    private static void validateGroup(GroupConfig group, StageResolver resolver) {
        if (group == null) {
            throw new IllegalStateException("pipeline.stages: group is null");
        }
        String mode = group.getExecutionMode();
        if (mode == null || (!"SYNC".equalsIgnoreCase(mode) && !"ASYNC".equalsIgnoreCase(mode))) {
            throw new IllegalStateException("pipeline.stages: group must have executionMode SYNC or ASYNC");
        }
        for (Object child : group.getChildrenAsList()) {
            if (child instanceof String) {
                String name = ((String) child).trim();
                if (name.isEmpty()) {
                    throw new IllegalStateException("pipeline.stages: group child activity name must be non-blank");
                }
                if (resolver != null && !resolver.canResolve(name)) {
                    throw new IllegalStateException("pipeline.stages: activity '" + name
                            + "' is not resolvable. Register a plugin for this activity name.");
                }
            } else if (child instanceof Map) {
                GroupConfig nested = MAPPER.convertValue(child, GroupConfig.class);
                validateGroup(nested, resolver);
            }
        }
    }
}
