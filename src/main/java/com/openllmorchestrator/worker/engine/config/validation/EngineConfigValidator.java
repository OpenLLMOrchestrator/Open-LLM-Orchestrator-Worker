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
