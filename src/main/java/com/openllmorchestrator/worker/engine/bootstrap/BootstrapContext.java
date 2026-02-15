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
package com.openllmorchestrator.worker.engine.bootstrap;

import com.openllmorchestrator.worker.engine.config.EngineFileConfig;
import com.openllmorchestrator.worker.engine.config.env.EnvConfig;
import com.openllmorchestrator.worker.engine.stage.StagePlan;
import com.openllmorchestrator.worker.engine.stage.activity.ActivityRegistry;

import java.util.Map;
import com.openllmorchestrator.worker.engine.stage.custom.CustomStageBucket;
import com.openllmorchestrator.worker.engine.stage.predefined.PredefinedPluginBucket;
import com.openllmorchestrator.worker.engine.stage.resolver.StageResolver;

import lombok.Getter;
import lombok.Setter;

/** Mutable context passed through bootstrap steps. */
@Getter
@Setter
public class BootstrapContext {
    private EnvConfig envConfig;
    private EngineFileConfig config;
    private PredefinedPluginBucket predefinedBucket;
    private CustomStageBucket customBucket;
    private ActivityRegistry activityRegistry;
    private StageResolver resolver;
    /** Single plan (legacy); used when plans map is not set. */
    private StagePlan plan;
    /** Named pipeline plans: pipeline name → StagePlan. When set, use getStagePlan(name) at runtime. */
    private Map<String, StagePlan> plans;
}
