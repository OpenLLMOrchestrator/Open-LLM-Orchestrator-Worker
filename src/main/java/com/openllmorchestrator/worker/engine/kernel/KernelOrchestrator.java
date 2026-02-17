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
package com.openllmorchestrator.worker.engine.kernel;

import com.openllmorchestrator.worker.engine.config.FeatureFlag;
import com.openllmorchestrator.worker.engine.config.FeatureFlags;
import com.openllmorchestrator.worker.engine.contract.ExecutionContext;
import com.openllmorchestrator.worker.engine.contract.KernelExecutionOutcome;
import com.openllmorchestrator.worker.engine.kernel.execution.AsyncGroupExecutor;
import com.openllmorchestrator.worker.engine.kernel.execution.ConditionalGroupExecutor;
import com.openllmorchestrator.worker.engine.kernel.execution.GroupExecutor;
import com.openllmorchestrator.worker.engine.runtime.EngineRuntime;
import com.openllmorchestrator.worker.engine.kernel.execution.PlanExecutorGroupExecutor;
import com.openllmorchestrator.worker.engine.kernel.execution.SyncGroupExecutor;
import com.openllmorchestrator.worker.engine.kernel.interceptor.ExecutionInterceptorChain;
import com.openllmorchestrator.worker.engine.kernel.interceptor.ExecutionInterceptor;
import com.openllmorchestrator.worker.engine.stage.StageGroupSpec;
import com.openllmorchestrator.worker.engine.stage.StagePlan;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Kernel: execution-state driven, deterministic, graph-capable, snapshot-aware.
 * <ul>
 *   <li><b>Execution-state driven</b>: Advances from explicit {@link ExecutionState} (plan + context + completed set).</li>
 *   <li><b>Deterministic</b>: Next group is chosen only from plan and completed set (ready set); no non-deterministic APIs.</li>
 *   <li><b>Graph-capable</b>: Plan groups may declare {@link StageGroupSpec#getDependsOnGroupIndices()}; ready = all deps completed.</li>
 *   <li><b>Snapshot-aware</b>: {@link ExecutionState#snapshot()} yields {@link ExecutionSnapshot} for observability/recovery.</li>
 * </ul>
 */
@Slf4j
public class KernelOrchestrator {
    private final StageInvoker stageInvoker;
    private final List<GroupExecutor> executors;
    private final ExecutionInterceptorChain interceptorChain;

    public KernelOrchestrator(StageInvoker stageInvoker) {
        this(stageInvoker, null);
    }

    /**
     * @param interceptors Kernel-level interceptors (SnapshotWriter, AuditWriter, Tracer, etc.). Non-optional layer; use no-op chain when empty.
     */
    public KernelOrchestrator(StageInvoker stageInvoker, List<ExecutionInterceptor> interceptors) {
        this.stageInvoker = stageInvoker;
        this.interceptorChain = interceptors != null && !interceptors.isEmpty()
                ? new ExecutionInterceptorChain(interceptors)
                : ExecutionInterceptorChain.noOp();
        this.executors = List.of(
                new PlanExecutorGroupExecutor(this::execute),
                new ConditionalGroupExecutor(this::execute),
                new SyncGroupExecutor(),
                new AsyncGroupExecutor()
        );
    }

    /**
     * Execute plan against context. State-driven: one group at a time from the deterministic ready set.
     * If a stage calls {@link ExecutionContext#requestSuspendForSignal()}, returns {@link KernelExecutionOutcome#suspended(long)}
     * so the workflow can await an {@link com.openllmorchestrator.worker.engine.contract.ExecutionSignal} and resume.
     */
    public KernelExecutionOutcome execute(StagePlan plan, ExecutionContext context) {
        ExecutionState state = new ExecutionState(plan, context);
        while (!state.isDone()) {
            List<Integer> ready = state.getReadyGroupIndices();
            if (ready.isEmpty()) {
                log.warn("No ready groups but not done; completed={}", state.getCompletedGroupIndices());
                break;
            }
            int next = ready.get(0);
            StageGroupSpec spec = plan.getGroups().get(next);
            log.info("---- Executing Stage Group {} ----", next);
            executeGroup(spec, context, next);
            state.markCompleted(next);
            if (context.isPipelineBreakRequested()) {
                log.info("Pipeline break requested; stopping further execution.");
                return KernelExecutionOutcome.breakRequested();
            }
            FeatureFlags flags = EngineRuntime.getFeatureFlags();
            if (flags != null && flags.isEnabled(FeatureFlag.HUMAN_SIGNAL) && context.isSuspendRequestedForSignal()) {
                long stepId = context.getVersionedState() != null ? context.getVersionedState().getStepId() : 0L;
                log.info("Suspend requested for signal (stepId={}); workflow will await signal.", stepId);
                return KernelExecutionOutcome.suspended(stepId);
            }
        }
        return KernelExecutionOutcome.completed();
    }

    private void executeGroup(StageGroupSpec spec, ExecutionContext context, int groupIndex) {
        for (GroupExecutor ex : executors) {
            if (ex.supports(spec)) {
                ex.execute(spec, stageInvoker, context, groupIndex, interceptorChain);
                return;
            }
        }
        throw new IllegalStateException("No executor for group");
    }
}

