/*
 * Copyright 2026 Open LLM Orchestrator contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.openllmorchestrator.worker.engine.config.validation;

import com.openllmorchestrator.worker.engine.config.EngineFileConfig;
import com.openllmorchestrator.worker.engine.config.pipeline.AllowedPluginTypes;
import com.openllmorchestrator.worker.engine.config.pipeline.NodeConfig;
import com.openllmorchestrator.worker.engine.capability.resolver.CapabilityResolver;

import java.util.HashSet;
import java.util.Set;

import com.openllmorchestrator.worker.engine.config.pipeline.PipelineSection;

import java.util.Map;

/** Validates pipeline tree (when using root): types, cycles, and that every PLUGIN node is resolvable. */
public final class PipelineNodeValidator implements ConfigValidator {
    @Override
    public void validate(EngineFileConfig config, CapabilityResolver resolver) {
        Map<String, PipelineSection> effective = config.getPipelinesEffective();
        for (Map.Entry<String, PipelineSection> e : effective.entrySet()) {
            PipelineSection section = e.getValue();
            if (section == null) continue;
            if (section.getCapabilities() != null && !section.getCapabilities().isEmpty()) {
                continue; // capabilities-based config validated by PipelineCapabilitiesValidator
            }
            if (section.getRoot() != null) {
                validateNode(section.getRoot(), section.getDefaultTimeoutSeconds(), new HashSet<>(), resolver);
                continue;
            }
            if (section.getRootByCapability() != null && !section.getRootByCapability().isEmpty()) {
                for (Map.Entry<String, NodeConfig> entry : section.getRootByCapability().entrySet()) {
                    String capabilityName = entry.getKey();
                    NodeConfig node = entry.getValue();
                    if (node == null) {
                        throw new IllegalStateException("Pipeline rootByCapability['" + capabilityName + "'] is null");
                    }
                    if (!node.isGroup()) {
                        throw new IllegalStateException("Pipeline rootByCapability['" + capabilityName + "'] must be GROUP, got: " + node.getType());
                    }
                    validateNode(node, section.getDefaultTimeoutSeconds(), new HashSet<>(), resolver);
                }
            }
        }
    }

    private static void validateNode(NodeConfig node, int defaultTimeoutSeconds,
                                     Set<NodeConfig> visited, CapabilityResolver resolver) {
        if (node == null) {
            throw new IllegalStateException("Pipeline node is null");
        }
        if (!visited.add(node)) {
            throw new IllegalStateException("Pipeline contains a cycle");
        }
        if (node.isPlugin()) {
            validatePluginNode(node, resolver);
            return;
        }
        if (node.isGroup()) {
            validateGroupNode(node, defaultTimeoutSeconds, visited, resolver);
            return;
        }
        throw new IllegalStateException("Pipeline node type must be GROUP or PLUGIN (STAGE accepted for backward compatibility), got: " + node.getType());
    }

    private static void validatePluginNode(NodeConfig node, CapabilityResolver resolver) {
        if (node.getName() == null || node.getName().isBlank()) {
            throw new IllegalStateException("PLUGIN node must have a non-blank 'name' (class name to call, e.g. FQCN)");
        }
        String pluginType = node.getPluginType();
        if (pluginType == null || pluginType.isBlank()) {
            throw new IllegalStateException("PLUGIN node must have 'pluginType' (one of: " + AllowedPluginTypes.all() + "). Node name: " + node.getName());
        }
        if (!AllowedPluginTypes.isAllowed(pluginType)) {
            throw new IllegalStateException("PLUGIN pluginType '" + pluginType + "' is not allowed. Must be one of: " + AllowedPluginTypes.all() + ". Node name: " + node.getName());
        }
        if (node.getTimeoutSeconds() != null && node.getTimeoutSeconds() <= 0) {
            throw new IllegalStateException("PLUGIN timeoutSeconds must be positive: " + node.getName());
        }
        if (resolver != null && !resolver.canResolve(node.getName())) {
            throw new IllegalStateException("Pipeline references unresolvable capability '" + node.getName()
                    + "'. Ensure the class is registered (plugin implementation with FQCN).");
        }
    }

    private static void validateGroupNode(NodeConfig node, int defaultTimeoutSeconds,
                                         Set<NodeConfig> visited, CapabilityResolver resolver) {
        String mode = node.getExecutionMode();
        if (mode == null || (!"SYNC".equalsIgnoreCase(mode) && !"ASYNC".equalsIgnoreCase(mode))) {
            throw new IllegalStateException("GROUP node must have executionMode SYNC or ASYNC");
        }
        // Each group may have at most one plugin of type PLUGIN_IF and at most one PLUGIN_ITERATOR (applies to whole group).
        int ifCount = 0;
        int iteratorCount = 0;
        int forkCount = 0;
        int joinCount = 0;
        for (NodeConfig child : node.getChildren()) {
            validateNode(child, defaultTimeoutSeconds, visited, resolver);
            if (child.isPlugin() && child.getPluginType() != null) {
                String pt = child.getPluginType();
                if (AllowedPluginTypes.PLUGIN_IF.equals(pt)) ifCount++;
                else if (AllowedPluginTypes.PLUGIN_ITERATOR.equals(pt)) iteratorCount++;
                else if (AllowedPluginTypes.FORK.equals(pt)) forkCount++;
                else if (AllowedPluginTypes.JOIN.equals(pt)) joinCount++;
            }
        }
        if (ifCount > 1) {
            throw new IllegalStateException("GROUP may have at most one plugin of type " + AllowedPluginTypes.PLUGIN_IF + "; found " + ifCount);
        }
        if (iteratorCount > 1) {
            throw new IllegalStateException("GROUP may have at most one plugin of type " + AllowedPluginTypes.PLUGIN_ITERATOR + "; found " + iteratorCount);
        }
        if (forkCount > 1) {
            throw new IllegalStateException("ASYNC GROUP may have at most one " + AllowedPluginTypes.FORK + " plugin; found " + forkCount);
        }
        if (joinCount > 1) {
            throw new IllegalStateException("ASYNC GROUP may have at most one " + AllowedPluginTypes.JOIN + " plugin; found " + joinCount);
        }
    }
}

