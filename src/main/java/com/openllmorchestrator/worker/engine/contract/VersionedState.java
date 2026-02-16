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

import lombok.Getter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Immutable versioned execution state. Every stage reads previous state and the kernel produces
 * new state (this object); stepId increments so we get replay, diff, time-travel, and audit.
 * <ul>
 *   <li>Sync: one stage → withNextStep(mergedState) → stepId + 1</li>
 *   <li>Async: N parallel stages → withNextStepAfterAsync(mergedState, N) → stepId + N (highest among them)</li>
 * </ul>
 */
@Getter
public class VersionedState {
    private final String executionId;
    private final long stepId;
    private final Map<String, Object> state;
    private final ExecutionMetadata metadata;

    public VersionedState(String executionId, long stepId, Map<String, Object> state, ExecutionMetadata metadata) {
        this.executionId = executionId;
        this.stepId = stepId;
        this.state = state != null ? Collections.unmodifiableMap(new HashMap<>(state)) : Map.of();
        this.metadata = metadata != null ? metadata : ExecutionMetadata.minimal(ExecutionMode.LIVE);
    }

    /**
     * Initial state for a new run: stepId 0, empty state.
     */
    public static VersionedState initial(String executionId, ExecutionMetadata metadata) {
        return new VersionedState(
                executionId != null ? executionId : "",
                0L,
                Map.of(),
                metadata
        );
    }

    /**
     * For activity context when we only have the state map (no executionId/stepId in activity).
     */
    public static VersionedState fromStateMap(Map<String, Object> state) {
        return new VersionedState("", -1L, state, ExecutionMetadata.minimal(ExecutionMode.LIVE));
    }

    /**
     * Next state after a sync stage: kernel merges plugin output into state, stepId + 1.
     */
    public VersionedState withNextStep(Map<String, Object> newState) {
        return new VersionedState(executionId, stepId + 1, newState != null ? newState : this.state, metadata);
    }

    /**
     * Next state after an async group: merged state, stepId = current + branchCount (highest among parallel branches).
     */
    public VersionedState withNextStepAfterAsync(Map<String, Object> newState, int branchCount) {
        long nextStepId = stepId + Math.max(1, branchCount);
        return new VersionedState(executionId, nextStepId, newState != null ? newState : this.state, metadata);
    }

    public boolean isFromActivity() {
        return stepId < 0;
    }
}

