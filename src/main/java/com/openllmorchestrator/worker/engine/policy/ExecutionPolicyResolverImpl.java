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

import java.util.Collections;
import java.util.List;

/**
 * Default policy resolver: no restrictions. Register a custom resolver at bootstrap when POLICY_ENGINE is enabled.
 */
public final class ExecutionPolicyResolverImpl implements ExecutionPolicyResolver {

    private static final ExecutionPolicy NO_RESTRICTIONS = new ExecutionPolicy() {
        @Override
        public List<String> getAllowedModels() { return Collections.emptyList(); }
        @Override
        public List<String> getToolWhitelist() { return Collections.emptyList(); }
        @Override
        public Double getBudgetCap() { return null; }
        @Override
        public Integer getMaxTokenCap() { return null; }
        @Override
        public Integer getMaxIterationCap() { return null; }
    };

    @Override
    public ExecutionPolicy resolve(ExecutionContext context) {
        return NO_RESTRICTIONS;
    }
}

