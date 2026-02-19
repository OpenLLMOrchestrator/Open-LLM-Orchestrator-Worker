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
package com.openllmorchestrator.worker.engine.kernel.feature;

import com.openllmorchestrator.worker.engine.config.FeatureFlag;

import java.util.List;

/**
 * Defines the order in which feature handlers run at every node traversal.
 * Order is fixed at bootstrap so the execution hierarchy persists.
 */
public final class FeatureHandlerOrder {

    /** Default order for feature handlers (e.g. observability first, then security, then policy, etc.). */
    private static final List<FeatureFlag> DEFAULT_ORDER = List.of(
            FeatureFlag.VERSIONED_STATE,
            FeatureFlag.STAGE_RESULT_ENVELOPE,
            FeatureFlag.EXECUTION_SNAPSHOT,
            FeatureFlag.INTERCEPTORS,
            FeatureFlag.OUTPUT_CONTRACT,
            FeatureFlag.SECURITY_HARDENING,
            FeatureFlag.POLICY_ENGINE,
            FeatureFlag.BUDGET_GUARDRAIL,
            FeatureFlag.PLAN_SAFETY_VALIDATION,
            FeatureFlag.DETERMINISM_POLICY,
            FeatureFlag.HUMAN_SIGNAL,
            FeatureFlag.STREAMING,
            FeatureFlag.AGENT_CONTEXT,
            FeatureFlag.CHECKPOINTABLE_STAGE,
            FeatureFlag.EXECUTION_GRAPH_EXPORT
    );

    private FeatureHandlerOrder() {}

    public static List<FeatureFlag> getDefaultOrder() {
        return DEFAULT_ORDER;
    }
}
