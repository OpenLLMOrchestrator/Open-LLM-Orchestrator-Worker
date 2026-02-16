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
package com.openllmorchestrator.worker.workflow;

import com.openllmorchestrator.worker.engine.contract.ExecutionCommand;
import com.openllmorchestrator.worker.engine.contract.ExecutionSignal;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

import java.util.Map;

@WorkflowInterface
public interface CoreWorkflow {

    /** Execute the pipeline and return the accumulated output (includes "result" from LLM) as the response. */
    @WorkflowMethod
    Map<String, Object> execute(ExecutionCommand command);

    /** Receive external signal to resume after suspend (e.g. human approval, manual override, compliance ack). */
    @SignalMethod
    void receiveSignal(ExecutionSignal signal);
}

