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
import com.openllmorchestrator.worker.contract.StageMetadata;
import com.openllmorchestrator.worker.contract.StageResult;
import com.openllmorchestrator.worker.engine.contract.VersionedState;
import com.openllmorchestrator.worker.engine.runtime.EngineRuntime;
import com.openllmorchestrator.worker.engine.kernel.StageInvoker;
import com.openllmorchestrator.worker.engine.kernel.interceptor.ExecutionInterceptorChain;
import com.openllmorchestrator.worker.engine.kernel.interceptor.StageContext;
import com.openllmorchestrator.worker.engine.stage.AsyncCompletionPolicy;
import com.openllmorchestrator.worker.engine.stage.StageDefinition;
import com.openllmorchestrator.worker.engine.stage.StageExecutionMode;
import com.openllmorchestrator.worker.engine.stage.StageGroupSpec;
import io.temporal.workflow.Promise;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public final class AsyncGroupExecutor implements GroupExecutor {
    @Override
    public boolean supports(StageGroupSpec spec) {
        return spec != null && spec.getDefinitions() != null && !spec.getDefinitions().isEmpty()
                && spec.getDefinitions().get(0).getExecutionMode() == StageExecutionMode.ASYNC;
    }

    @Override
    public void execute(StageGroupSpec spec, StageInvoker invoker, ExecutionContext context,
                        int groupIndex, ExecutionInterceptorChain interceptorChain) {
        List<StageDefinition> group = spec.getDefinitions();
        VersionedState stateBefore = context.getVersionedState();
        for (StageDefinition def : group) {
            StageContext stageCtx = StageContext.from(groupIndex, def, stateBefore, context);
            interceptorChain.beforeStage(stageCtx);
        }
        AsyncCompletionPolicy policy = spec.getAsyncPolicy() != null ? spec.getAsyncPolicy() : AsyncCompletionPolicy.ALL;
        List<Promise<StageResult>> promises = new ArrayList<>();
        for (StageDefinition def : group) {
            log.info("Scheduling ASYNC stage: {}", def.getName());
            promises.add(invoker.invokeAsync(def, context));
        }
        waitForPromises(policy, promises);
        List<String> names = new ArrayList<>(group.size());
        List<StageResult> results = new ArrayList<>(group.size());
        for (int i = 0; i < group.size(); i++) {
            StageDefinition def = group.get(i);
            names.add(def.getName());
            StageContext stageCtx = StageContext.from(groupIndex, def, stateBefore, context);
            try {
                StageResult r = promises.get(i).get();
                results.add(r);
                interceptorChain.afterStage(stageCtx, r);
            } catch (Exception e) {
                results.add(StageResult.builder().stageName(def.getName()).build());
                interceptorChain.onError(stageCtx, e);
            }
        }
        String taskQueue = group.isEmpty() ? null : group.get(0).getTaskQueue();
        java.time.Duration timeout = group.isEmpty() ? java.time.Duration.ofSeconds(30) : group.get(0).getTimeout();
        Map<String, Object> merged = invoker.invokeMerge(
                spec.getAsyncOutputMergePolicyName(), taskQueue, timeout, context, names, results);
        VersionedState current = context.getVersionedState();
        VersionedState next = current.withNextStepAfterAsync(merged != null ? merged : Map.of(), group.size());
        context.setVersionedState(next);
        boolean allRequestBreak = !results.isEmpty() && results.stream().allMatch(r -> r != null && r.isRequestPipelineBreak());
        if (allRequestBreak) {
            context.setPipelineBreakRequested(true);
        }
        FeatureFlags flags = EngineRuntime.getFeatureFlags();
        for (int i = 0; i < results.size(); i++) {
            StageResult r = results.get(i);
            if (flags != null && flags.isEnabled(FeatureFlag.STAGE_RESULT_ENVELOPE) && r.getMetadata() == null && i < group.size()) {
                StageDefinition def = group.get(i);
                r.setMetadata(StageMetadata.builder()
                        .stageName(def.getName())
                        .stepId(next.getStepId())
                        .executionId(next.getExecutionId())
                        .stageBucketName(def.getStageBucketName())
                        .build());
            }
            log.info("Completed ASYNC stage: {} (stepId after async={})", r.getStageName(), next.getStepId());
        }
    }

    private static void waitForPromises(AsyncCompletionPolicy policy, List<Promise<StageResult>> promises) {
        switch (policy) {
            case ALL:
            case FIRST_FAILURE:
            case ALL_SETTLED:
                Promise.allOf(promises).get();
                break;
            case FIRST_SUCCESS:
                Promise.anyOf(promises).get();
                break;
            default:
                Promise.allOf(promises).get();
        }
    }
}

