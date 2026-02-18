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
package com.openllmorchestrator.worker.engine.config;

import com.openllmorchestrator.worker.engine.contract.ExecutionGraph;
import com.openllmorchestrator.worker.engine.capability.predefined.PredefinedCapabilities;

import java.util.List;

/**
 * Worker-side helpers for engine config: effective capability order (with predefined fallback),
 * execution graph, and feature flags. Config POJOs and serialization live in engine-config module.
 */
public final class EngineConfigRuntime {

    private EngineConfigRuntime() {}

    /** Effective capability order: from config if set, otherwise predefined order. */
    public static List<String> getCapabilityOrderEffective(EngineFileConfig config) {
        List<String> order = config.getCapabilityOrderEffective();
        if (order != null && !order.isEmpty()) {
            return order;
        }
        return PredefinedCapabilities.orderedNames();
    }

    /** Build execution graph from config (linear order with predefined fallback when order is empty). */
    public static ExecutionGraph getExecutionGraphEffective(EngineFileConfig config) {
        return ExecutionGraph.fromLinearOrder(getCapabilityOrderEffective(config));
    }

    /** Build feature flags from config enabled feature names. */
    public static FeatureFlags getFeatureFlagsEffective(EngineFileConfig config) {
        return FeatureFlags.fromNames(config.getEnabledFeatureNames());
    }
}
