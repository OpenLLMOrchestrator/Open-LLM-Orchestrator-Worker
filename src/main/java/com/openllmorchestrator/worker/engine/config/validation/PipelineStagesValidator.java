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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openllmorchestrator.worker.engine.config.EngineFileConfig;
import com.openllmorchestrator.worker.engine.config.pipeline.GroupConfig;
import com.openllmorchestrator.worker.engine.config.pipeline.PipelineSection;
import com.openllmorchestrator.worker.engine.config.pipeline.CapabilityBlockConfig;
import com.openllmorchestrator.worker.engine.capability.resolver.CapabilityResolver;

import java.util.List;
import java.util.Map;

/** Validates pipeline.capabilities: capability names, group executionMode, and that every activity name is resolvable. */
public final class PipelineStagesValidator implements ConfigValidator {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public void validate(EngineFileConfig config, CapabilityResolver resolver) {
        Map<String, PipelineSection> effective = config.getPipelinesEffective();
        for (Map.Entry<String, PipelineSection> e : effective.entrySet()) {
            PipelineSection section = e.getValue();
            if (section == null) continue;
            List<CapabilityBlockConfig> capabilities = section.getCapabilities();
            if (capabilities == null || capabilities.isEmpty()) {
                continue;
            }
            for (CapabilityBlockConfig block : capabilities) {
                if (block == null) {
                    throw new IllegalStateException("pipeline.capabilities contains a null capability block");
                }
                if (block.getCapability() == null || block.getCapability().isBlank()) {
                    throw new IllegalStateException("pipeline.capabilities: each capability block must have a non-blank 'capability' name");
                }
                for (GroupConfig group : block.getGroupsSafe()) {
                    validateGroup(group, resolver);
                }
            }
        }
    }

    private static void validateGroup(GroupConfig group, CapabilityResolver resolver) {
        if (group == null) {
            throw new IllegalStateException("pipeline.capabilities: group is null");
        }
        String mode = group.getExecutionMode();
        if (mode == null || (!"SYNC".equalsIgnoreCase(mode) && !"ASYNC".equalsIgnoreCase(mode))) {
            throw new IllegalStateException("pipeline.capabilities: group must have executionMode SYNC or ASYNC");
        }
        for (Object child : group.getChildrenAsList()) {
            if (child instanceof String) {
                String name = ((String) child).trim();
                if (name.isEmpty()) {
                    throw new IllegalStateException("pipeline.capabilities: group child activity name must be non-blank");
                }
                if (resolver != null && !resolver.canResolve(name)) {
                    throw new IllegalStateException("pipeline.capabilities: activity '" + name
                            + "' is not resolvable. Register a plugin for this activity name.");
                }
            } else if (child instanceof Map) {
                GroupConfig nested = MAPPER.convertValue(child, GroupConfig.class);
                validateGroup(nested, resolver);
            }
        }
    }
}

