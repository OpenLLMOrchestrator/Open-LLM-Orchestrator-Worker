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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Result of kernel execution. When suspended, workflow awaits ExecutionSignal then resumes. Serializable. */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class KernelExecutionOutcome {

    private boolean completed;
    private boolean suspended;
    private long suspendedAtStepId;
    /** True when pipeline was stopped because one or more activities requested break. */
    private boolean breakRequested;

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
