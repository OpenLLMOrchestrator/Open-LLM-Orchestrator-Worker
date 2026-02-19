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
import com.openllmorchestrator.worker.engine.capability.CapabilityDefinition;
import lombok.Builder;
import lombok.Getter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Immutable context for the execution interceptor at a single capability (before/after/onError).
 * Optional executionContext is set when building from ExecutionContext so feature execution plugins can access full context.
 */
@Getter
@Builder
public class CapabilityContext {
    private final String executionId;
    private final long stepId;
    private final int groupIndex;
    private final CapabilityDefinition capabilityDefinition;
    private final Map<String, Object> stateBefore;
    private final ExecutionMode executionMode;
    private final String pipelineName;
    /** Optional: set when built from ExecutionContext so feature plugins can read/write accumulated output etc. */
    private final ExecutionContext executionContext;

    public Map<String, Object> getStateBefore() {
        return stateBefore != null ? Collections.unmodifiableMap(stateBefore) : Map.of();
    }

    public static CapabilityContext from(String executionId, long stepId, int groupIndex,
                                         CapabilityDefinition definition, VersionedState versionedState,
                                         ExecutionMode executionMode, String pipelineName) {
        return CapabilityContext.builder()
                .executionId(executionId != null ? executionId : "")
                .stepId(stepId)
                .groupIndex(groupIndex)
                .capabilityDefinition(definition)
                .stateBefore(versionedState != null && versionedState.getState() != null
                        ? new HashMap<>(versionedState.getState())
                        : Map.of())
                .executionMode(executionMode != null ? executionMode : ExecutionMode.LIVE)
                .pipelineName(pipelineName != null ? pipelineName : "")
                .executionContext(null)
                .build();
    }

    public static CapabilityContext from(int groupIndex, CapabilityDefinition definition,
                                         VersionedState versionedState, ExecutionContext context) {
        String executionId = versionedState != null ? versionedState.getExecutionId() : "";
        long stepId = versionedState != null ? versionedState.getStepId() : 0L;
        ExecutionMode mode = context != null ? context.getExecutionMode() : ExecutionMode.LIVE;
        String pipeline = context != null && context.getCommand() != null && context.getCommand().getPipelineName() != null
                ? context.getCommand().getPipelineName()
                : "default";
        return CapabilityContext.builder()
                .executionId(executionId)
                .stepId(stepId)
                .groupIndex(groupIndex)
                .capabilityDefinition(definition)
                .stateBefore(versionedState != null && versionedState.getState() != null
                        ? new HashMap<>(versionedState.getState())
                        : Map.of())
                .executionMode(mode)
                .pipelineName(pipeline)
                .executionContext(context)
                .build();
    }
}
