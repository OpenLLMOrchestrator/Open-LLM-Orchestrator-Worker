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

import com.openllmorchestrator.worker.engine.contract.ExecutionContext;

import java.util.List;

/**
 * Central policy resolver. Unifies tenant policy, guardrails, and rate limits so policy is not distributed.
 * When {@link com.openllmorchestrator.worker.engine.config.FeatureFlag#POLICY_ENGINE} is enabled, kernel/stages
 * resolve model selection by tenant tier, tool whitelist per user, budget cap, max iteration, and max token cap here.
 */
public interface ExecutionPolicyResolver {

    /** Resolve effective policy for this execution (tenant, user, pipeline). */
    ExecutionPolicy resolve(ExecutionContext context);

    /** Effective policy for one run: model selection, tool whitelist, caps. */
    interface ExecutionPolicy {
        /** Allowed model id(s) for this tenant/user tier; empty = no restriction. */
        List<String> getAllowedModels();
        /** Tool/capability names allowed for this user; empty = no restriction. */
        List<String> getToolWhitelist();
        /** Max cost (e.g. USD) for this run; null = no cap. */
        Double getBudgetCap();
        /** Max tokens for this run; null = no cap. */
        Integer getMaxTokenCap();
        /** Max iterations (e.g. iterative block); null = no cap. */
        Integer getMaxIterationCap();
    }
}

