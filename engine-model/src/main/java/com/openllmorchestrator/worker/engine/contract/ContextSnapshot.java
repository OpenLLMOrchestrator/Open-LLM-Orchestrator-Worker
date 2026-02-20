/*
 * Copyright 2026 Open LLM Orchestrator contributors.
 */
package com.openllmorchestrator.worker.engine.contract;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.openllmorchestrator.worker.contract.DeterminismPolicy;
import lombok.Getter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** Immutable snapshot of execution context at a point in time. Serializable. */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
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
