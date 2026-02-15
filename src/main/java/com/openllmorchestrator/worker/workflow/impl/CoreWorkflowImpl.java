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
package com.openllmorchestrator.worker.workflow.impl;

import com.openllmorchestrator.worker.engine.contract.ExecutionCommand;
import com.openllmorchestrator.worker.engine.contract.ExecutionContext;
import com.openllmorchestrator.worker.engine.kernel.KernelOrchestrator;
import com.openllmorchestrator.worker.engine.kernel.StageInvoker;
import com.openllmorchestrator.worker.engine.runtime.EngineRuntime;
import com.openllmorchestrator.worker.engine.stage.StagePlan;
import com.openllmorchestrator.worker.workflow.CoreWorkflow;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Uses one-time bootstrapped plan and invoker; no config read per execution.
 * Returns the accumulated output (including "result" from the LLM plugin) as the response.
 */
public class CoreWorkflowImpl implements CoreWorkflow {

    private static final Logger log = Workflow.getLogger(CoreWorkflowImpl.class);

    @Override
    public Map<String, Object> execute(ExecutionCommand command) {
        ExecutionContext context = ExecutionContext.from(command);
        String pipelineName = command.getPipelineName() != null && !command.getPipelineName().isBlank()
                ? command.getPipelineName()
                : "default";
        StagePlan plan = EngineRuntime.getStagePlan(pipelineName);
        log.info("Executing pipeline: {}", pipelineName);
        StageInvoker invoker = new StageInvoker();
        KernelOrchestrator kernel = new KernelOrchestrator(invoker);
        kernel.execute(plan, context);
        return new HashMap<>(context.getAccumulatedOutput());
    }
}
