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
 * Result of kernel execution. When suspended, workflow awaits {@link ExecutionSignal} then resumes.
 */
@Getter
@Builder
public class KernelExecutionOutcome {

    private final boolean completed;
    private final boolean suspended;
    private final long suspendedAtStepId;
    /** True when pipeline was stopped because one or more activities requested break (ASYNC: all in group requested). */
    private final boolean breakRequested;

    public static KernelExecutionOutcome completed() {
        return KernelExecutionOutcome.builder().completed(true).suspended(false).suspendedAtStepId(0L).breakRequested(false).build();
    }

    public static KernelExecutionOutcome suspended(long atStepId) {
        return KernelExecutionOutcome.builder().completed(false).suspended(true).suspendedAtStepId(atStepId).breakRequested(false).build();
    }

    public static KernelExecutionOutcome breakRequested() {
        return KernelExecutionOutcome.builder().completed(true).suspended(false).suspendedAtStepId(0L).breakRequested(true).build();
    }
}

