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
package com.openllmorchestrator.worker.engine.kernel.interceptor;

import com.openllmorchestrator.worker.engine.contract.ExecutionContext;
import com.openllmorchestrator.worker.engine.contract.ExecutionMode;
import com.openllmorchestrator.worker.engine.contract.VersionedState;
import com.openllmorchestrator.worker.engine.stage.StageDefinition;
import lombok.Builder;
import lombok.Getter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Immutable context for the execution interceptor at a single stage (before/after/onError).
 * Gives interceptors a snapshot of execution id, step, group, definition, and state for
 * SnapshotWriter, AuditWriter, Tracer, Evaluator, CostMeter, etc.
 */
@Getter
@Builder
public class StageContext {
    private final String executionId;
    private final long stepId;
    private final int groupIndex;
    private final StageDefinition stageDefinition;
    /** State snapshot before this stage (read-only). */
    private final Map<String, Object> stateBefore;
    private final ExecutionMode executionMode;
    private final String pipelineName;

    public Map<String, Object> getStateBefore() {
        return stateBefore != null ? Collections.unmodifiableMap(stateBefore) : Map.of();
    }

    public static StageContext from(String executionId, long stepId, int groupIndex,
                                   StageDefinition definition, VersionedState versionedState,
                                   ExecutionMode executionMode, String pipelineName) {
        return StageContext.builder()
                .executionId(executionId != null ? executionId : "")
                .stepId(stepId)
                .groupIndex(groupIndex)
                .stageDefinition(definition)
                .stateBefore(versionedState != null && versionedState.getState() != null
                        ? new HashMap<>(versionedState.getState())
                        : Map.of())
                .executionMode(executionMode != null ? executionMode : ExecutionMode.LIVE)
                .pipelineName(pipelineName != null ? pipelineName : "")
                .build();
    }

    /** Build from execution context (reads executionMode and pipelineName from context). */
    public static StageContext from(int groupIndex, StageDefinition definition,
                                   VersionedState versionedState, ExecutionContext context) {
        String executionId = versionedState != null ? versionedState.getExecutionId() : "";
        long stepId = versionedState != null ? versionedState.getStepId() : 0L;
        ExecutionMode mode = context != null ? context.getExecutionMode() : ExecutionMode.LIVE;
        String pipeline = context != null && context.getCommand() != null && context.getCommand().getPipelineName() != null
                ? context.getCommand().getPipelineName()
                : "default";
        return from(executionId, stepId, groupIndex, definition, versionedState, mode, pipeline);
    }
}

