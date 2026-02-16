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
package com.openllmorchestrator.worker.engine.kernel.execution;

import com.openllmorchestrator.worker.engine.config.FeatureFlag;
import com.openllmorchestrator.worker.engine.config.FeatureFlags;
import com.openllmorchestrator.worker.engine.contract.ExecutionContext;
import com.openllmorchestrator.worker.engine.contract.PlannerContextKeys;
import com.openllmorchestrator.worker.contract.StageMetadata;
import com.openllmorchestrator.worker.contract.StageResult;
import com.openllmorchestrator.worker.engine.contract.VersionedState;
import com.openllmorchestrator.worker.engine.plan.PlanValidator;
import com.openllmorchestrator.worker.engine.plan.PlanValidator.PlanValidationException;
import com.openllmorchestrator.worker.engine.runtime.EngineRuntime;
import com.openllmorchestrator.worker.engine.kernel.StageInvoker;
import com.openllmorchestrator.worker.engine.kernel.interceptor.ExecutionInterceptorChain;
import com.openllmorchestrator.worker.engine.kernel.interceptor.StageContext;
import com.openllmorchestrator.worker.engine.stage.StageGroupSpec;
import com.openllmorchestrator.worker.engine.stage.StagePlan;
import com.openllmorchestrator.worker.engine.stage.predefined.PredefinedStages;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * Executes the dynamic plan stored by the PLANNER stage (under {@link PlannerContextKeys#KEY_DYNAMIC_PLAN}).
 * PLAN_EXECUTOR is not a plugin: when the kernel hits this group, it runs the stored sub-plan in place.
 * When called inside an iterator, this acts as an iterative plan executor (plan runs with current context each time).
 */
@Slf4j
public final class PlanExecutorGroupExecutor implements GroupExecutor {

    private final SubPlanRunner subPlanRunner;

    public PlanExecutorGroupExecutor(SubPlanRunner subPlanRunner) {
        this.subPlanRunner = subPlanRunner != null ? subPlanRunner : (plan, ctx) -> {};
    }

    @Override
    public boolean supports(StageGroupSpec spec) {
        if (spec == null || spec.getDefinitions() == null || spec.getDefinitions().isEmpty()) {
            return false;
        }
        return spec.getDefinitions().size() == 1
                && PredefinedStages.PLAN_EXECUTOR.equals(spec.getDefinitions().get(0).getStageBucketName());
    }

    @Override
    public void execute(StageGroupSpec spec, StageInvoker invoker, ExecutionContext context,
                        int groupIndex, ExecutionInterceptorChain interceptorChain) {
        StageContext stageCtx = StageContext.from(groupIndex, spec.getDefinitions().get(0), context.getVersionedState(), context);
        interceptorChain.beforeStage(stageCtx);
        try {
            Map<String, Object> accumulated = context.getAccumulatedOutput();
            Object raw = accumulated != null ? accumulated.get(PlannerContextKeys.KEY_DYNAMIC_PLAN) : null;
            StagePlan subPlan = raw instanceof StagePlan ? (StagePlan) raw : null;
            if (subPlan == null) {
                log.debug("PLAN_EXECUTOR: no dynamic plan in context (key={}); skipping", PlannerContextKeys.KEY_DYNAMIC_PLAN);
                StageResult empty = StageResult.builder().stageName(PredefinedStages.PLAN_EXECUTOR).build();
                interceptorChain.afterStage(stageCtx, empty);
                return;
            }
            FeatureFlags flags = EngineRuntime.getFeatureFlags();
            if (flags != null && flags.isEnabled(FeatureFlag.PLAN_SAFETY_VALIDATION)) {
                PlanValidator validator = EngineRuntime.getPlanValidator();
                if (validator != null) {
                    try {
                        validator.validate(subPlan, context);
                    } catch (PlanValidationException ex) {
                        throw new IllegalStateException("Plan validation failed: " + ex.getMessage(), ex);
                    }
                }
            }
            log.info("PLAN_EXECUTOR: executing dynamic plan with {} group(s)", subPlan.getGroups().size());
            subPlanRunner.run(subPlan, context);
            VersionedState after = context.getVersionedState();
            StageResult.StageResultBuilder resultBuilder = StageResult.builder()
                    .stageName(PredefinedStages.PLAN_EXECUTOR)
                    .output(context.getAccumulatedOutput())
                    .data(context.getAccumulatedOutput());
            if (flags != null && flags.isEnabled(FeatureFlag.STAGE_RESULT_ENVELOPE)) {
                resultBuilder.metadata(StageMetadata.builder()
                        .stageName(PredefinedStages.PLAN_EXECUTOR)
                        .stepId(after != null ? after.getStepId() : 0L)
                        .executionId(after != null ? after.getExecutionId() : null)
                        .stageBucketName(PredefinedStages.PLAN_EXECUTOR)
                        .build());
            }
            interceptorChain.afterStage(stageCtx, resultBuilder.build());
        } catch (Exception e) {
            interceptorChain.onError(stageCtx, e);
            throw e;
        }
    }

    /** Runs a sub-plan with the given context (e.g. kernel.execute(plan, context)). */
    @FunctionalInterface
    public interface SubPlanRunner {
        void run(StagePlan subPlan, ExecutionContext context);
    }
}

