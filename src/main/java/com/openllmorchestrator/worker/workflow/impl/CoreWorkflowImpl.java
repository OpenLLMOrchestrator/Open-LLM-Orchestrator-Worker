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
import com.openllmorchestrator.worker.engine.contract.ExecutionSignal;
import com.openllmorchestrator.worker.engine.contract.KernelExecutionOutcome;
import com.openllmorchestrator.worker.engine.kernel.KernelOrchestrator;
import com.openllmorchestrator.worker.engine.capability.CapabilityPlan;
import com.openllmorchestrator.worker.engine.kernel.CapabilityInvoker;
import com.openllmorchestrator.worker.engine.runtime.EngineRuntime;
import com.openllmorchestrator.worker.workflow.CoreWorkflow;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Uses one-time bootstrapped plan and invoker; no config read per execution.
 * Returns the accumulated output (including "result" from the LLM plugin) as the response.
 * <p><b>Suspend/resume</b>: If a stage requests suspend for signal, workflow awaits {@link #receiveSignal(ExecutionSignal)};
 * on signal, workflow injects it into context and continues kernel (re-run so the stage can read the signal).
 * <p><b>Deterministic</b>: Workflow code must not use non-deterministic APIs so that Temporal replay produces the same decisions.
 */
public class CoreWorkflowImpl implements CoreWorkflow {

    private static final Logger log = Workflow.getLogger(CoreWorkflowImpl.class);

    /** Set by {@link #receiveSignal(ExecutionSignal)}; workflow awaits until non-null when kernel is suspended. */
    private ExecutionSignal receivedSignal;

    @Override
    public Map<String, Object> execute(ExecutionCommand command) {
        ExecutionContext context = ExecutionContext.from(command, com.openllmorchestrator.worker.engine.runtime.EngineRuntime.getFeatureFlags());
        String pipelineName = command.getPipelineName() != null && !command.getPipelineName().isBlank()
                ? command.getPipelineName()
                : "default";
        CapabilityPlan plan = EngineRuntime.getCapabilityPlan(pipelineName);
        log.info("Executing pipeline: {}", pipelineName);
        CapabilityInvoker invoker = new CapabilityInvoker();
        KernelOrchestrator kernel = new KernelOrchestrator(invoker);
        KernelExecutionOutcome outcome = kernel.execute(plan, context);
        com.openllmorchestrator.worker.engine.config.FeatureFlags flags = com.openllmorchestrator.worker.engine.runtime.EngineRuntime.getFeatureFlags();
        while (flags != null && flags.isEnabled(com.openllmorchestrator.worker.engine.config.FeatureFlag.HUMAN_SIGNAL) && outcome.isSuspended()) {
            log.info("Workflow suspended at step {}; awaiting signal.", outcome.getSuspendedAtStepId());
            Workflow.await(() -> receivedSignal != null);
            context.setResumeSignal(receivedSignal);
            receivedSignal = null;
            outcome = kernel.execute(plan, context);
        }
        return new HashMap<>(context.getAccumulatedOutput());
    }

    @Override
    public void receiveSignal(ExecutionSignal signal) {
        this.receivedSignal = signal;
    }
}

