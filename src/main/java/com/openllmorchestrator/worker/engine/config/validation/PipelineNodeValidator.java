package com.openllmorchestrator.worker.engine.config.validation;

import com.openllmorchestrator.worker.engine.config.EngineFileConfig;
import com.openllmorchestrator.worker.engine.config.pipeline.NodeConfig;
import com.openllmorchestrator.worker.engine.stage.resolver.StageResolver;

import java.util.HashSet;
import java.util.Set;

/** Validates pipeline tree (when using root): types, cycles, and that every STAGE is resolvable. */
public final class PipelineNodeValidator implements ConfigValidator {
    @Override
    public void validate(EngineFileConfig config, StageResolver resolver) {
        if (config.getPipeline().getStages() != null && !config.getPipeline().getStages().isEmpty()) {
            return; // stages-based config validated by PipelineStagesValidator
        }
        if (config.getPipeline().getRoot() == null) {
            return;
        }
        validateNode(config.getPipeline().getRoot(), config.getPipeline().getDefaultTimeoutSeconds(),
                new HashSet<>(), resolver);
    }

    private static void validateNode(NodeConfig node, int defaultTimeoutSeconds,
                                     Set<NodeConfig> visited, StageResolver resolver) {
        if (node == null) {
            throw new IllegalStateException("Pipeline node is null");
        }
        if (!visited.add(node)) {
            throw new IllegalStateException("Pipeline contains a cycle");
        }
        if (node.isStage()) {
            validateStageNode(node, resolver);
            return;
        }
        if (node.isGroup()) {
            validateGroupNode(node, defaultTimeoutSeconds, visited, resolver);
            return;
        }
        throw new IllegalStateException("Pipeline node type must be GROUP or STAGE, got: " + node.getType());
    }

    private static void validateStageNode(NodeConfig node, StageResolver resolver) {
        if (node.getName() == null || node.getName().isBlank()) {
            throw new IllegalStateException("STAGE node must have a non-blank 'name'");
        }
        if (node.getTimeoutSeconds() != null && node.getTimeoutSeconds() <= 0) {
            throw new IllegalStateException("STAGE timeoutSeconds must be positive: " + node.getName());
        }
        if (resolver != null && !resolver.canResolve(node.getName())) {
            throw new IllegalStateException("Pipeline references unresolvable stage '" + node.getName()
                    + "'. Predefined stages need a plugin in stagePlugins; custom stages must be in the custom plugin bucket.");
        }
    }

    private static void validateGroupNode(NodeConfig node, int defaultTimeoutSeconds,
                                         Set<NodeConfig> visited, StageResolver resolver) {
        String mode = node.getExecutionMode();
        if (mode == null || (!"SYNC".equalsIgnoreCase(mode) && !"ASYNC".equalsIgnoreCase(mode))) {
            throw new IllegalStateException("GROUP node must have executionMode SYNC or ASYNC");
        }
        for (NodeConfig child : node.getChildren()) {
            validateNode(child, defaultTimeoutSeconds, visited, resolver);
        }
    }
}
