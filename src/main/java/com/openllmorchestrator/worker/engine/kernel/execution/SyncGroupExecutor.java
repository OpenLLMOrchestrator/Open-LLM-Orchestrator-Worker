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
import com.openllmorchestrator.worker.contract.CapabilityMetadata;
import com.openllmorchestrator.worker.contract.CapabilityResult;
import com.openllmorchestrator.worker.engine.runtime.EngineRuntime;
import com.openllmorchestrator.worker.engine.contract.VersionedState;
import com.openllmorchestrator.worker.engine.capability.CapabilityDefinition;
import com.openllmorchestrator.worker.engine.capability.CapabilityExecutionMode;
import com.openllmorchestrator.worker.engine.capability.CapabilityGroupSpec;
import com.openllmorchestrator.worker.engine.kernel.CapabilityInvoker;
import com.openllmorchestrator.worker.engine.kernel.interceptor.CapabilityContext;
import com.openllmorchestrator.worker.engine.kernel.interceptor.ExecutionInterceptorChain;
import com.openllmorchestrator.worker.engine.kernel.merge.OutputMergePolicy;
import com.openllmorchestrator.worker.engine.kernel.merge.PutAllMergePolicy;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public final class SyncGroupExecutor implements GroupExecutor {
    private static final OutputMergePolicy SYNC_MERGE = PutAllMergePolicy.INSTANCE;

    @Override
    public boolean supports(CapabilityGroupSpec spec) {
        return spec != null && spec.getDefinitions() != null && !spec.getDefinitions().isEmpty()
                && spec.getDefinitions().get(0).getExecutionMode() == CapabilityExecutionMode.SYNC;
    }

    @Override
    public void execute(CapabilityGroupSpec spec, CapabilityInvoker invoker, ExecutionContext context,
                       int groupIndex, ExecutionInterceptorChain interceptorChain) {
        VersionedState current = context.getVersionedState();
        Map<String, Object> state = new HashMap<>(context.getAccumulatedOutput());
        for (CapabilityDefinition def : spec.getDefinitions()) {
            CapabilityContext capCtx = CapabilityContext.from(groupIndex, def, current, context);
            interceptorChain.beforeCapability(capCtx);
            CapabilityResult result;
            try {
                log.info("Executing SYNC capability: {}", def.getName());
                result = invoker.invokeSync(def, context);
                Map<String, Object> output = result.getOutput() != null ? result.getOutput() : Map.of();
                SYNC_MERGE.merge(state, output, def.getName());
                current = current.withNextStep(state);
                context.setVersionedState(current);
                FeatureFlags flags = EngineRuntime.getFeatureFlags();
                if (flags != null && flags.isEnabled(FeatureFlag.STAGE_RESULT_ENVELOPE) && result.getMetadata() == null) {
                    result.setMetadata(CapabilityMetadata.builder()
                            .capabilityName(def.getName())
                            .stepId(current.getStepId())
                            .executionId(current.getExecutionId())
                            .capabilityBucketName(def.getCapabilityBucketName())
                            .build());
                }
                state = new HashMap<>(current.getState());
                interceptorChain.afterCapability(capCtx, result);
                if (result.isRequestPipelineBreak()) {
                    context.setPipelineBreakRequested(true);
                    log.info("Completed SYNC capability: {} (stepId={}); pipeline break requested, stopping group.", result.getCapabilityName(), current.getStepId());
                    break;
                }
                log.info("Completed SYNC capability: {} (stepId={})", result.getCapabilityName(), current.getStepId());
            } catch (Exception e) {
                interceptorChain.onError(capCtx, e);
                throw e;
            }
        }
    }
}

