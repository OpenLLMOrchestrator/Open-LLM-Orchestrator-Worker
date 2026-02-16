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
import lombok.*;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionCommand {

    private String operation;
    private String tenantId;
    private String userId;
    private Map<String, Object> metadata;
    /** Initial input map for the pipeline; every plugin can read this (read-only). */
    private Map<String, Object> input;
    /** Pipeline name to run (e.g. "chat", "document-extraction"). When null/blank, "default" is used. */
    private String pipelineName;
    /** Optional execution id for versioned state (replay, audit). When null, workflow may generate one. */
    private String executionId;
    /** When set, kernel resumes from this step (checkpoint). Requires state at stepId; checkpointable stages may receive resumeFrom(stepId). */
    private Long resumeFromStepId;
    /** When set, kernel branches from this step. Checkpointable stages may receive branchFrom(stepId). */
    private Long branchFromStepId;
    /** Optional agent id for durable agent identity (see AgentContext). */
    private String agentId;
    /** Optional persona name for the agent. */
    private String persona;
    /** When set, kernel applies deterministic behavior (freeze model params, persist tool/retrieval outputs for replay). */
    private DeterminismPolicy determinismPolicy;
    /** Optional seed for reproducible randomness when determinism policy is enabled; kernel/store can use for replay. */
    private Long randomnessSeed;
}

