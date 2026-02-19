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

import com.openllmorchestrator.worker.engine.capability.CapabilityPlan;
import com.openllmorchestrator.worker.engine.capability.activity.ActivityRegistry;
import com.openllmorchestrator.worker.engine.capability.custom.CustomCapabilityBucket;
import com.openllmorchestrator.worker.engine.capability.predefined.PredefinedPluginBucket;
import com.openllmorchestrator.worker.engine.capability.resolver.CapabilityResolver;
import com.openllmorchestrator.worker.engine.config.EngineFileConfig;
import com.openllmorchestrator.worker.engine.config.env.EnvConfig;

import java.util.Map;

import lombok.Getter;
import lombok.Setter;

/**
 * Mutable context passed through bootstrap steps. All products (config, resolver, plans) are
 * stateless: no transactional or request-scoped data. Execution hierarchy is built once and
 * reused for the container lifecycle.
 */
@Getter
@Setter
public class BootstrapContext {
    /** Task queue name for this bootstrap; used to load queue-specific config and register execution tree. */
    private String queueName;
    private EnvConfig envConfig;
    private EngineFileConfig config;
    private PredefinedPluginBucket predefinedBucket;
    private CustomCapabilityBucket customBucket;
    private ActivityRegistry activityRegistry;
    private ActivityRegistry compatibleActivityRegistry;
    private CapabilityResolver resolver;
    private CapabilityPlan plan;
    /** Named pipeline plans: pipeline name → CapabilityPlan. When set, use getCapabilityPlan(name) at runtime. */
    private Map<String, CapabilityPlan> plans;
}

