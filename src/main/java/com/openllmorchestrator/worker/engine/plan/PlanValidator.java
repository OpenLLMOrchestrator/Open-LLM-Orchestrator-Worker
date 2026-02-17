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
package com.openllmorchestrator.worker.engine.plan;

import com.openllmorchestrator.worker.engine.contract.ExecutionContext;
import com.openllmorchestrator.worker.engine.capability.CapabilityPlan;

import java.util.List;

/**
 * Validates a dynamic plan before execution. When {@link com.openllmorchestrator.worker.engine.config.FeatureFlag#PLAN_SAFETY_VALIDATION}
 * is enabled, PLAN_EXECUTOR runs the validator before executing the plan.
 * Ensures: allowed stages only, max depth, no recursive infinite loops, no unauthorized stage injection.
 */
public interface PlanValidator {

    /**
     * Validate the plan. Throws if invalid; otherwise kernel may proceed.
     *
     * @param plan    dynamic plan from PLANNER
     * @param context current execution context (tenant, user, allowed list from policy)
     */
    void validate(CapabilityPlan plan, ExecutionContext context) throws PlanValidationException;

    /** Result of validation: allowed stage names, max depth. */
    interface ValidationRules {
        List<String> getAllowedStageNames();
        int getMaxDepth();
    }

    class PlanValidationException extends Exception {
        public PlanValidationException(String message) {
            super(message);
        }
        public PlanValidationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

