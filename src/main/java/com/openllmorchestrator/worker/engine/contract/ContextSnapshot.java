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
package com.openllmorchestrator.worker.engine.contract;

import com.openllmorchestrator.worker.contract.DeterminismPolicy;
import lombok.Getter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Immutable snapshot of execution context (originalInput, accumulatedOutput, state, executionMode, versionedState) at a point in time.
 * Used for observability, recovery, replay, and snapshot-aware kernel behavior. Values are deep-copied.
 */
@Getter
public class ContextSnapshot {
    private final Map<String, Object> originalInput;
    private final Map<String, Object> accumulatedOutput;
    private final Map<String, Object> state;
    private final ExecutionMode executionMode;
    private final VersionedState versionedState;
    private final DeterminismPolicy determinismPolicy;

    public ContextSnapshot(Map<String, Object> originalInput, Map<String, Object> accumulatedOutput,
                          Map<String, Object> state) {
        this(originalInput, accumulatedOutput, state, null, null, null);
    }

    public ContextSnapshot(Map<String, Object> originalInput, Map<String, Object> accumulatedOutput,
                          Map<String, Object> state, ExecutionMode executionMode) {
        this(originalInput, accumulatedOutput, state, executionMode, null, null);
    }

    public ContextSnapshot(Map<String, Object> originalInput, Map<String, Object> accumulatedOutput,
                          Map<String, Object> state, ExecutionMode executionMode, VersionedState versionedState) {
        this(originalInput, accumulatedOutput, state, executionMode, versionedState, null);
    }

    public ContextSnapshot(Map<String, Object> originalInput, Map<String, Object> accumulatedOutput,
                          Map<String, Object> state, ExecutionMode executionMode, VersionedState versionedState,
                          DeterminismPolicy determinismPolicy) {
        this.originalInput = originalInput != null ? Collections.unmodifiableMap(new HashMap<>(originalInput)) : Map.of();
        this.accumulatedOutput = accumulatedOutput != null ? Collections.unmodifiableMap(new HashMap<>(accumulatedOutput)) : Map.of();
        this.state = state != null ? Collections.unmodifiableMap(new HashMap<>(state)) : Map.of();
        this.executionMode = executionMode != null ? executionMode : ExecutionMode.LIVE;
        this.versionedState = versionedState;
        this.determinismPolicy = determinismPolicy != null ? determinismPolicy : DeterminismPolicy.NONE;
    }
}

