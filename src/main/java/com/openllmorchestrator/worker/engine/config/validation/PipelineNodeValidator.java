package com.openllmorchestrator.worker.engine.config.validation;

import com.openllmorchestrator.worker.engine.config.EngineFileConfig;
import com.openllmorchestrator.worker.engine.config.pipeline.AllowedPluginTypes;
import com.openllmorchestrator.worker.engine.config.pipeline.NodeConfig;
import com.openllmorchestrator.worker.engine.stage.resolver.StageResolver;

import java.util.HashSet;
import java.util.Set;

import com.openllmorchestrator.worker.engine.config.pipeline.PipelineSection;

import java.util.Map;

/** Validates pipeline tree (when using root): types, cycles, and that every STAGE is resolvable. */
public final class PipelineNodeValidator implements ConfigValidator {
    @Override
    public void validate(EngineFileConfig config, StageResolver resolver) {
        Map<String, PipelineSection> effective = config.getPipelinesEffective();
        for (Map.Entry<String, PipelineSection> e : effective.entrySet()) {
            PipelineSection section = e.getValue();
            if (section == null) continue;
            if (section.getStages() != null && !section.getStages().isEmpty()) {
                continue; // stages-based config validated by PipelineStagesValidator
            }
            if (section.getRoot() != null) {
                validateNode(section.getRoot(), section.getDefaultTimeoutSeconds(), new HashSet<>(), resolver);
                continue;
            }
            if (section.getRootByStage() != null && !section.getRootByStage().isEmpty()) {
                for (Map.Entry<String, NodeConfig> entry : section.getRootByStage().entrySet()) {
                    String stageName = entry.getKey();
                    NodeConfig node = entry.getValue();
                    if (node == null) {
                        throw new IllegalStateException("Pipeline rootByStage['" + stageName + "'] is null");
                    }
                    if (!node.isGroup()) {
                        throw new IllegalStateException("Pipeline rootByStage['" + stageName + "'] must be GROUP, got: " + node.getType());
                    }
                    validateNode(node, section.getDefaultTimeoutSeconds(), new HashSet<>(), resolver);
                }
            }
        }
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
            throw new IllegalStateException("STAGE node must have a non-blank 'name' (class name to call, e.g. FQCN)");
        }
        String pluginType = node.getPluginType();
        if (pluginType == null || pluginType.isBlank()) {
            throw new IllegalStateException("STAGE node must have 'pluginType' (one of: " + AllowedPluginTypes.all() + "). Node name: " + node.getName());
        }
        if (!AllowedPluginTypes.isAllowed(pluginType)) {
            throw new IllegalStateException("STAGE pluginType '" + pluginType + "' is not allowed. Must be one of: " + AllowedPluginTypes.all() + ". Node name: " + node.getName());
        }
        if (node.getTimeoutSeconds() != null && node.getTimeoutSeconds() <= 0) {
            throw new IllegalStateException("STAGE timeoutSeconds must be positive: " + node.getName());
        }
        if (resolver != null && !resolver.canResolve(node.getName())) {
            throw new IllegalStateException("Pipeline references unresolvable stage '" + node.getName()
                    + "'. Ensure the class is registered (plugin implementation with FQCN).");
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
