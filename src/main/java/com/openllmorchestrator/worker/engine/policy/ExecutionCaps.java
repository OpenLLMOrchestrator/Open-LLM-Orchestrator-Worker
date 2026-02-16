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
package com.openllmorchestrator.worker.engine.policy;

import lombok.Builder;
import lombok.Getter;

/**
 * Kernel-level execution caps. When {@link com.openllmorchestrator.worker.engine.config.FeatureFlag#BUDGET_GUARDRAIL}
 * is enabled, kernel checks after each stage and stops execution if any cap is exceeded.
 */
@Getter
@Builder
public class ExecutionCaps {

    private final Double maxCost;
    private final Integer maxTokens;
    private final Integer maxIterations;

    /** Check if execution should stop given current accumulated cost/tokens/iterations. */
    public boolean exceeded(double currentCost, int currentTokens, int currentIterations) {
        if (maxCost != null && currentCost > maxCost) return true;
        if (maxTokens != null && currentTokens > maxTokens) return true;
        if (maxIterations != null && currentIterations > maxIterations) return true;
        return false;
    }
}

