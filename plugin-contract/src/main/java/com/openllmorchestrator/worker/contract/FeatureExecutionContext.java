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
package com.openllmorchestrator.worker.contract;

import java.util.Map;

/**
 * Context passed to {@link FeatureExecutionPlugin} at pre/post during feature execution.
 * Exposes the current feature, capability, step state, and pipeline context so plugins
 * can read and optionally write context data in bootstrap-defined order.
 */
public interface FeatureExecutionContext {

    /** Feature flag name (e.g. STREAMING, HUMAN_SIGNAL) this execution is for. */
    String getFeatureName();

    /** Current step id (monotonic per execution). */
    long getStepId();

    /** Execution id (workflow run). */
    String getExecutionId();

    /** Group index in the capability plan. */
    int getGroupIndex();

    /** Current capability name (stage) being executed. */
    String getCapabilityName();

    /** State snapshot before this capability (accumulated output up to this step). */
    Map<String, Object> getStateBefore();

    /** Pipeline name (e.g. default, chat). */
    String getPipelineName();

    /** Read-only: initial pipeline input. */
    Map<String, Object> getOriginalInput();

    /** Read-only: output accumulated so far (previous stages). */
    Map<String, Object> getAccumulatedOutput();

    /** Mutable: current plugin output (merged by kernel after stage). */
    Map<String, Object> getCurrentPluginOutput();

    /** Write current plugin output (convenience). */
    void putOutput(String key, Object value);
}
