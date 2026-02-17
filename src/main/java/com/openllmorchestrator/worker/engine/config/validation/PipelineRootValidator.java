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
import com.openllmorchestrator.worker.engine.capability.resolver.CapabilityResolver;

import com.openllmorchestrator.worker.engine.config.pipeline.PipelineSection;

import java.util.Map;

/** Validates pipeline section(s): either root or capabilities/stages must be set per pipeline; defaultTimeoutSeconds positive. */
public final class PipelineRootValidator implements ConfigValidator {
    @Override
    public void validate(EngineFileConfig config, CapabilityResolver resolver) {
        Map<String, PipelineSection> effective = config.getPipelinesEffective();
        if (effective.isEmpty()) {
            throw new IllegalStateException("config must have pipelines with at least one pipeline defined");
        }
        for (Map.Entry<String, PipelineSection> e : effective.entrySet()) {
            String name = e.getKey();
            PipelineSection section = e.getValue();
            if (section == null) {
                throw new IllegalStateException("config.pipelines['" + name + "'] is null");
            }
            boolean hasStages = section.getStages() != null && !section.getStages().isEmpty();
            boolean hasRoot = section.getRoot() != null;
            boolean hasRootByStage = section.getRootByStage() != null && !section.getRootByStage().isEmpty();
            if (!hasStages && !hasRoot && !hasRootByStage) {
                throw new IllegalStateException("Pipeline '" + name + "' must have root, rootByStage, or stages");
            }
            if (section.getDefaultTimeoutSeconds() <= 0) {
                throw new IllegalStateException("Pipeline '" + name + "': defaultTimeoutSeconds must be positive");
            }
        }
    }
}

