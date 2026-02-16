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
 * Plugin-facing view of execution context. Implemented by the engine's ExecutionContext.
 * Plugins read input and accumulated output, write current output; optional: request suspend, break, read agent/determinism.
 */
public interface PluginContext {

    /** Read-only: initial pipeline input (same for all plugins). */
    Map<String, Object> getOriginalInput();

    /** Read-only: output from previous stages merged so far. */
    Map<String, Object> getAccumulatedOutput();

    /** Mutable: current plugin output (merged by kernel after stage). */
    Map<String, Object> getCurrentPluginOutput();

    /** Write current plugin output (convenience). */
    void putOutput(String key, Object value);

    /** Generic state get (e.g. asyncStageResults for merge handlers). */
    Object get(String key);

    /** Generic state put. */
    void put(String key, Object value);

    /** Request workflow suspend until external signal (e.g. human approval). No-op when HUMAN_SIGNAL disabled. */
    void requestSuspendForSignal();

    /** Request to break the pipeline (no further stages). */
    void requestPipelineBreak();

    /** Whether pipeline break was requested (by this or kernel). */
    boolean isPipelineBreakRequested();

    /** Optional agent context when AGENT_CONTEXT enabled; null otherwise. */
    AgentContext getAgentContext();

    /** Determinism policy when DETERMINISM_POLICY enabled; NONE otherwise. */
    DeterminismPolicy getDeterminismPolicy();

    /** Optional randomness seed for reproducible runs. */
    Long getRandomnessSeed();

    /** Optional pipeline name (e.g. for model resolution from pipeline id). */
    String getPipelineName();
}
