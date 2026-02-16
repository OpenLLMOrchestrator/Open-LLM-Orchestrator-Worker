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

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * External signal to suspend/resume workflow (e.g. human approval, manual override, compliance gate).
 * Workflow suspends when kernel reports suspend; client sends this signal to resume.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionSignal {

    /** Signal type, e.g. {@link #TYPE_HUMAN_APPROVAL}. */
    private String type;
    /** Opaque payload (approval result, override data, etc.). */
    private Object payload;

    public static final String TYPE_HUMAN_APPROVAL = "HUMAN_APPROVAL";
    public static final String TYPE_MANUAL_OVERRIDE = "MANUAL_OVERRIDE";
    public static final String TYPE_COMPLIANCE_ACK = "COMPLIANCE_ACK";
}

