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

import lombok.Builder;
import lombok.Getter;

/**
 * Immutable metadata for an execution run (mode, tenant, user, etc.).
 * Used by {@link VersionedState} for replay, audit, and branching.
 */
@Getter
@Builder
public class ExecutionMetadata {
    private final ExecutionMode executionMode;
    private final String tenantId;
    private final String userId;
    private final String pipelineName;

    public static ExecutionMetadata from(ExecutionCommand command) {
        return from(command, ExecutionMode.LIVE);
    }

    public static ExecutionMetadata from(ExecutionCommand command, ExecutionMode mode) {
        if (command == null) {
            return ExecutionMetadata.builder().executionMode(mode != null ? mode : ExecutionMode.LIVE).build();
        }
        return ExecutionMetadata.builder()
                .executionMode(mode != null ? mode : ExecutionMode.LIVE)
                .tenantId(command.getTenantId())
                .userId(command.getUserId())
                .pipelineName(command.getPipelineName())
                .build();
    }

    public static ExecutionMetadata minimal(ExecutionMode mode) {
        return ExecutionMetadata.builder()
                .executionMode(mode != null ? mode : ExecutionMode.LIVE)
                .build();
    }
}

