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
import com.openllmorchestrator.worker.contract.CapabilityMetadata;
import com.openllmorchestrator.worker.contract.CapabilityResult;
import com.openllmorchestrator.worker.engine.contract.VersionedState;
import com.openllmorchestrator.worker.engine.plan.PlanValidator;
import com.openllmorchestrator.worker.engine.plan.PlanValidator.PlanValidationException;
import com.openllmorchestrator.worker.engine.runtime.EngineRuntime;
import com.openllmorchestrator.worker.engine.kernel.CapabilityInvoker;
import com.openllmorchestrator.worker.engine.kernel.interceptor.ExecutionInterceptorChain;
import com.openllmorchestrator.worker.engine.kernel.interceptor.CapabilityContext;
import com.openllmorchestrator.worker.engine.capability.CapabilityGroupSpec;
import com.openllmorchestrator.worker.engine.capability.CapabilityPlan;
import com.openllmorchestrator.worker.engine.capability.predefined.PredefinedCapabilities;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * Executes the dynamic plan stored by the PLANNER capability (under {@link PlannerContextKeys#KEY_DYNAMIC_PLAN}).
 * PLAN_EXECUTOR is not a plugin: when the kernel hits this group, it runs the stored sub-plan in place.
 * <p><b>Dynamic tree scope:</b> The execution tree for the dynamic plan is created and run only here—within
 * the scope of this capability. Same kernel, same context (accumulatedOutput, versionedState), same
 * interceptors and feature flags. No bootstrap-built hierarchy for dynamic content.
 * When called inside an iterator, this acts as an iterative plan executor (plan runs with current context each time).
 */
@Slf4j
public final class PlanExecutorGroupExecutor implements GroupExecutor {

    private final SubPlanRunner subPlanRunner;

    public PlanExecutorGroupExecutor(SubPlanRunner subPlanRunner) {
        this.subPlanRunner = subPlanRunner != null ? subPlanRunner : (plan, ctx) -> {};
    }

    @Override
    public boolean supports(CapabilityGroupSpec spec) {
        if (spec == null || spec.getDefinitions() == null || spec.getDefinitions().isEmpty()) {
            return false;
        }
        return spec.getDefinitions().size() == 1
                && PredefinedCapabilities.PLAN_EXECUTOR.equals(spec.getDefinitions().get(0).getCapabilityBucketName());
    }

    @Override
    public void execute(CapabilityGroupSpec spec, CapabilityInvoker invoker, ExecutionContext context,
                        int groupIndex, ExecutionInterceptorChain interceptorChain) {
        CapabilityContext capabilityCtx = CapabilityContext.from(groupIndex, spec.getDefinitions().get(0), context.getVersionedState(), context);
        interceptorChain.beforeCapability(capabilityCtx);
        try {
            Map<String, Object> accumulated = context.getAccumulatedOutput();
            Object raw = accumulated != null ? accumulated.get(PlannerContextKeys.KEY_DYNAMIC_PLAN) : null;
            CapabilityPlan subPlan = raw instanceof CapabilityPlan ? (CapabilityPlan) raw : null;
            if (subPlan == null) {
                log.debug("PLAN_EXECUTOR: no dynamic plan in context (key={}); skipping", PlannerContextKeys.KEY_DYNAMIC_PLAN);
                CapabilityResult empty = CapabilityResult.builder().capabilityName(PredefinedCapabilities.PLAN_EXECUTOR).build();
                interceptorChain.afterCapability(capabilityCtx, empty);
                return;
            }
            String queueName = context.getQueueName();
            FeatureFlags flags = EngineRuntime.getFeatureFlags(queueName);
            if (flags != null && flags.isEnabled(FeatureFlag.PLAN_SAFETY_VALIDATION)) {
                PlanValidator validator = EngineRuntime.getPlanValidator(queueName);
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
            CapabilityResult.CapabilityResultBuilder resultBuilder = CapabilityResult.builder()
                    .capabilityName(PredefinedCapabilities.PLAN_EXECUTOR)
                    .output(context.getAccumulatedOutput())
                    .data(context.getAccumulatedOutput());
            if (flags != null && flags.isEnabled(FeatureFlag.STAGE_RESULT_ENVELOPE)) {
                resultBuilder.metadata(CapabilityMetadata.builder()
                        .capabilityName(PredefinedCapabilities.PLAN_EXECUTOR)
                        .stepId(after != null ? after.getStepId() : 0L)
                        .executionId(after != null ? after.getExecutionId() : null)
                        .capabilityBucketName(PredefinedCapabilities.PLAN_EXECUTOR)
                        .build());
            }
            interceptorChain.afterCapability(capabilityCtx, resultBuilder.build());
        } catch (Exception e) {
            interceptorChain.onError(capabilityCtx, e);
            throw e;
        }
    }

    /** Runs a sub-plan with the given context (e.g. kernel.execute(plan, context)). */
    @FunctionalInterface
    public interface SubPlanRunner {
        void run(CapabilityPlan subPlan, ExecutionContext context);
    }
}

