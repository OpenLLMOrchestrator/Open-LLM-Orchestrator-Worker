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

import com.openllmorchestrator.worker.contract.AgentContext;
import com.openllmorchestrator.worker.contract.DeterminismPolicy;
import com.openllmorchestrator.worker.contract.PluginContext;
import lombok.Getter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Execution context for one workflow run. Context is <b>versioned</b>: each stage reads previous
 * {@link VersionedState} and the kernel produces new state (stepId increments). Enables replay, diff,
 * time-travel, and audit.
 * <p>Plugins read {@link #getAccumulatedOutput()} (current versioned state) and write to
 * {@link #getCurrentPluginOutput()}; kernel merges and sets next {@link VersionedState}.
 */
@Getter
public class ExecutionContext implements PluginContext {

    private final ExecutionCommand command;
    /** General state; use for backward compatibility. */
    private final Map<String, Object> state = new HashMap<>();

    /**
     * How this run is executed. Default LIVE. Plugins/kernel can branch (e.g. DRY_RUN = build prompts only).
     */
    private ExecutionMode executionMode = ExecutionMode.LIVE;

    /** Versioned state (immutable). Kernel replaces this after each stage with next step. */
    private VersionedState versionedState;

    /** When set by a stage, kernel returns suspended and workflow awaits {@link ExecutionSignal}. */
    private volatile boolean suspendRequestedForSignal;
    /** After workflow receives signal and resumes, this holds the signal (e.g. human approval payload). */
    private volatile ExecutionSignal resumeSignal;
    /** When true, kernel will stop the pipeline after this group (set by executor when stage(s) request break; for ASYNC, only when all in group request). */
    private volatile boolean pipelineBreakRequested;

    /** Optional durable agent identity (agentId, persona, memoryStore). Enables agents to persist across sessions. */
    private AgentContext agentContext;

    /** Read-only: initial input for the pipeline (same for all plugins). */
    private final Map<String, Object> originalInput;
    /** Mutable: current plugin's output (merged into versioned state after stage; for ASYNC see merge policy). */
    private final Map<String, Object> currentPluginOutput;

    /** When set, feature-flag checks are applied (e.g. HUMAN_SIGNAL); when null, optional features are no-op. */
    private final FeatureFlagsProvider featureFlagsProvider;

    public ExecutionContext(ExecutionCommand command) {
        this(command, null);
    }

    public ExecutionContext(ExecutionCommand command, FeatureFlagsProvider featureFlagsProvider) {
        this.command = command;
        this.featureFlagsProvider = featureFlagsProvider;
        Map<String, Object> input = command != null && command.getInput() != null ? command.getInput() : null;
        this.originalInput = input != null ? Collections.unmodifiableMap(new HashMap<>(input)) : Collections.emptyMap();
        this.currentPluginOutput = new HashMap<>();
        String executionId = command != null && command.getExecutionId() != null && !command.getExecutionId().isBlank()
                ? command.getExecutionId()
                : UUID.randomUUID().toString();
        this.versionedState = VersionedState.initial(executionId, ExecutionMetadata.from(command, executionMode));
    }

    /** For activity: originalInput and accumulatedOutput are read-only snapshots; currentPluginOutput is mutable. */
    public ExecutionContext(Map<String, Object> originalInput, Map<String, Object> accumulatedOutput) {
        this.command = null;
        this.featureFlagsProvider = null;
        this.originalInput = originalInput != null ? Collections.unmodifiableMap(new HashMap<>(originalInput)) : Collections.emptyMap();
        this.currentPluginOutput = new HashMap<>();
        this.versionedState = VersionedState.fromStateMap(accumulatedOutput);
    }

    /**
     * Current accumulated output (read-only). Returns the state map from the current {@link VersionedState}.
     */
    public Map<String, Object> getAccumulatedOutput() {
        return versionedState != null ? versionedState.getState() : Map.of();
    }

    /**
     * Set the next versioned state after kernel merges stage output (kernel calls this after each stage).
     */
    public void setVersionedState(VersionedState next) {
        if (next != null) {
            this.versionedState = next;
        }
    }

    /** For workflow: from command; originalInput from command.getInput(), accumulatedOutput empty. */
    public static ExecutionContext from(ExecutionCommand command) {
        return new ExecutionContext(command, null);
    }

    /** For workflow with feature flags: only enabled features execute. */
    public static ExecutionContext from(ExecutionCommand command, FeatureFlagsProvider featureFlagsProvider) {
        return new ExecutionContext(command, featureFlagsProvider);
    }

    /** For activity: context with given read-only maps and empty currentPluginOutput for plugin to fill. */
    public static ExecutionContext forActivity(Map<String, Object> originalInput, Map<String, Object> accumulatedOutput) {
        return new ExecutionContext(originalInput, accumulatedOutput);
    }

    public void setExecutionMode(ExecutionMode executionMode) {
        this.executionMode = executionMode != null ? executionMode : ExecutionMode.LIVE;
    }

    /** Request workflow suspend until an external {@link ExecutionSignal} is received (e.g. human approval). No-op when HUMAN_SIGNAL feature is disabled. */
    public void requestSuspendForSignal() {
        if (featureFlagsProvider != null && !featureFlagsProvider.isEnabled("HUMAN_SIGNAL")) return;
        this.suspendRequestedForSignal = true;
    }

    public boolean isSuspendRequestedForSignal() {
        return suspendRequestedForSignal;
    }

    public ExecutionSignal getResumeSignal() {
        return resumeSignal;
    }

    /** Set by workflow after receiving signal; stages can read this on resume. */
    public void setResumeSignal(ExecutionSignal resumeSignal) {
        this.resumeSignal = resumeSignal;
    }

    /** Request to break the pipeline (no further stages). Activity/stage sets this or returns StageResult with requestPipelineBreak=true. */
    public void requestPipelineBreak() {
        this.pipelineBreakRequested = true;
    }

    public boolean isPipelineBreakRequested() {
        return pipelineBreakRequested;
    }

    /** Set by kernel executors when the group outcome is to break (SYNC: any stage requested; ASYNC: all in group requested). */
    public void setPipelineBreakRequested(boolean pipelineBreakRequested) {
        this.pipelineBreakRequested = pipelineBreakRequested;
    }

    /** Null when AGENT_CONTEXT feature is disabled. */
    public AgentContext getAgentContext() {
        if (featureFlagsProvider != null && !featureFlagsProvider.isEnabled("AGENT_CONTEXT")) return null;
        return agentContext;
    }

    public void setAgentContext(AgentContext agentContext) {
        this.agentContext = agentContext;
    }

    /** Effective determinism policy from command; {@link DeterminismPolicy#NONE} when not set or when DETERMINISM_POLICY feature is disabled. */
    @Override
    public DeterminismPolicy getDeterminismPolicy() {
        if (featureFlagsProvider != null && !featureFlagsProvider.isEnabled("DETERMINISM_POLICY")) return DeterminismPolicy.NONE;
        if (command != null && command.getDeterminismPolicy() != null) return command.getDeterminismPolicy();
        return DeterminismPolicy.NONE;
    }

    /** Optional randomness seed from command for reproducible runs when determinism is enabled. */
    public Long getRandomnessSeed() {
        return command != null ? command.getRandomnessSeed() : null;
    }

    public void put(String key, Object value) {
        state.put(key, value);
    }

    public Object get(String key) {
        return state.get(key);
    }

    /** Write current plugin output (convenience). */
    public void putOutput(String key, Object value) {
        currentPluginOutput.put(key, value);
    }

    @Override
    public String getPipelineName() {
        return command != null ? command.getPipelineName() : null;
    }

    /**
     * Snapshot of current context (originalInput, accumulatedOutput, state) for observability and snapshot-aware kernel.
     * In workflow, accumulatedOutput is mutable; this returns a copy at this point in time.
     */
    public ContextSnapshot snapshot() {
        return new ContextSnapshot(
                new HashMap<>(originalInput),
                new HashMap<>(getAccumulatedOutput()),
                new HashMap<>(state),
                executionMode,
                versionedState,
                getDeterminismPolicy()
        );
    }
}

