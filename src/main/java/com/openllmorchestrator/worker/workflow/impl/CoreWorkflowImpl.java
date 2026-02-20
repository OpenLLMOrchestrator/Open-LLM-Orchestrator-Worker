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

import com.openllmorchestrator.worker.engine.activity.DebugPushActivity;
import com.openllmorchestrator.worker.engine.contract.ExecutionCommand;
import com.openllmorchestrator.worker.engine.contract.ExecutionContext;
import com.openllmorchestrator.worker.engine.contract.ExecutionSignal;
import com.openllmorchestrator.worker.engine.contract.SharedFolderContextKeys;
import com.openllmorchestrator.worker.engine.contract.KernelExecutionOutcome;
import com.openllmorchestrator.worker.engine.kernel.KernelOrchestrator;
import com.openllmorchestrator.worker.engine.capability.CapabilityPlan;
import com.openllmorchestrator.worker.engine.kernel.CapabilityInvoker;
import com.openllmorchestrator.worker.engine.runtime.EngineRuntime;
import com.openllmorchestrator.worker.engine.kernel.interceptor.ExecutionInterceptorChain;
import com.openllmorchestrator.worker.workflow.CoreWorkflow;
import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.time.Duration;
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
        if (command.getQueueName() == null || command.getQueueName().isBlank()) {
            command.setQueueName(Workflow.getInfo().getTaskQueue());
        }
        String queueName = command.getQueueName();
        ExecutionContext context = ExecutionContext.from(command, EngineRuntime.getFeatureFlags(queueName));
        context.put(SharedFolderContextKeys.SHARED_FOLDER_PATH, EngineRuntime.getConfig(queueName).getSharedFolderPathEffective());
        String pipelineName = command.getPipelineName() != null && !command.getPipelineName().isBlank()
                ? command.getPipelineName()
                : "default";
        // Static flow: use immutable global plan (context.executionPlan stays null). Planner/debug phases create a copy in context before modifying.
        CapabilityPlan globalPlan = EngineRuntime.getCapabilityPlan(queueName, pipelineName);
        CapabilityPlan planToRun = context.getExecutionPlan() != null ? context.getExecutionPlan() : globalPlan;

        // When DEBUGGER FF is enabled and command has debug=true and debugID (same level as tenantId, userId, operation), put stub/plan in context so DebuggerFeatureHandler can push before/after every node.
        com.openllmorchestrator.worker.engine.config.FeatureFlags flagsForDebug = EngineRuntime.getFeatureFlags(queueName);
        if (flagsForDebug != null && flagsForDebug.isEnabled(com.openllmorchestrator.worker.engine.config.FeatureFlag.DEBUGGER)) {
            boolean debug = Boolean.TRUE.equals(command.getDebug());
            String debugID = command.getDebugID() != null && !command.getDebugID().isBlank() ? command.getDebugID().trim() : null;
            if (debug && debugID != null) {
                ActivityOptions options = ActivityOptions.newBuilder()
                        .setTaskQueue(Workflow.getInfo().getTaskQueue())
                        .setStartToCloseTimeout(Duration.ofSeconds(30))
                        .build();
                DebugPushActivity debugPush = Workflow.newActivityStub(DebugPushActivity.class, options);
                context.put(com.openllmorchestrator.worker.engine.kernel.feature.DebuggerFeatureHandler.STATE_KEY_DEBUG_ID, debugID);
                context.put(com.openllmorchestrator.worker.engine.kernel.feature.DebuggerFeatureHandler.STATE_KEY_DEBUG_PUSH_ACTIVITY, debugPush);
                context.put(com.openllmorchestrator.worker.engine.kernel.feature.DebuggerFeatureHandler.STATE_KEY_DEBUG_PLAN, planToRun);
            }
        }

        log.info("Executing pipeline: {} (queue: {})", pipelineName, queueName);
        CapabilityInvoker invoker = new CapabilityInvoker();
        ExecutionInterceptorChain chain = EngineRuntime.getExecutionInterceptorChain(queueName);
        KernelOrchestrator kernel = new KernelOrchestrator(invoker, chain);
        KernelExecutionOutcome outcome = kernel.execute(planToRun, context);
        com.openllmorchestrator.worker.engine.config.FeatureFlags flags = EngineRuntime.getFeatureFlags(queueName);
        while (flags != null && flags.isEnabled(com.openllmorchestrator.worker.engine.config.FeatureFlag.HUMAN_SIGNAL) && outcome.isSuspended()) {
            log.info("Workflow suspended at step {}; awaiting signal.", outcome.getSuspendedAtStepId());
            Workflow.await(() -> receivedSignal != null);
            context.setResumeSignal(receivedSignal);
            receivedSignal = null;
            planToRun = context.getExecutionPlan() != null ? context.getExecutionPlan() : globalPlan;
            outcome = kernel.execute(planToRun, context);
        }
        return new HashMap<>(context.getAccumulatedOutput());
    }

    @Override
    public void receiveSignal(ExecutionSignal signal) {
        this.receivedSignal = signal;
    }
}

