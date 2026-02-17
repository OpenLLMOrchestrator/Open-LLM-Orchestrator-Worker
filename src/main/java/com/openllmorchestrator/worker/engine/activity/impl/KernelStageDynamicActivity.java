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
package com.openllmorchestrator.worker.engine.activity.impl;

import com.openllmorchestrator.worker.engine.contract.ExecutionContext;
import com.openllmorchestrator.worker.contract.ContractVersion;
import com.openllmorchestrator.worker.contract.CapabilityResult;
import com.openllmorchestrator.worker.engine.runtime.EngineRuntime;
import com.openllmorchestrator.worker.contract.OutputContract;
import com.openllmorchestrator.worker.contract.OutputContractValidator;
import com.openllmorchestrator.worker.contract.OutputContractViolationException;
import com.openllmorchestrator.worker.contract.CapabilityHandler;
import com.openllmorchestrator.worker.engine.capability.predefined.PredefinedCapabilities;
import com.openllmorchestrator.worker.engine.capability.resolver.CapabilityResolver;
import io.temporal.activity.Activity;
import io.temporal.activity.DynamicActivity;
import io.temporal.common.converter.EncodedValues;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles stage activities invoked with a custom activity type (e.g. "RETRIEVAL::VectorStoreRetrievalPlugin")
 * so the Temporal UI shows Stage::Plugin instead of "Execute". Dispatches to the same logic as
 * {@link KernelStageActivityImpl} using the stage name from the first argument.
 */
@Slf4j
public class KernelStageDynamicActivity implements DynamicActivity {

    @Override
    public Object execute(EncodedValues args) {
        String activityType = Activity.getExecutionContext().getInfo().getActivityType();
        log.debug(">>> [START] Activity type: {} | Thread: {}", activityType, Thread.currentThread().getName());

        String stageName = args.get(0, String.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> originalInput = args.get(1, Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> accumulatedOutput = args.get(2, Map.class);

        CapabilityResolver resolver = EngineRuntime.getCapabilityResolver();
        CapabilityHandler handler = resolver.resolve(stageName);
        if (handler == null) {
            if (PredefinedCapabilities.isPredefined(stageName)) {
                throw new IllegalStateException("Predefined stage '" + stageName
                        + "' has no plugin registered. Register a handler for this stage.");
            }
            throw new IllegalStateException("Stage or activity '" + stageName
                    + "' could not be resolved. Register it in the activity registry (plugin name) or custom bucket.");
        }
        ContractVersion.requireCompatible(handler);
        ExecutionContext context = ExecutionContext.forActivity(
                originalInput != null ? originalInput : Map.of(),
                accumulatedOutput != null ? accumulatedOutput : Map.of());
        handler.execute(context);
        KernelStageActivityImpl.validateOutputContract(handler, context.getCurrentPluginOutput(), stageName);
        log.debug("<<< [END] Activity type: {} | Thread: {}", activityType, Thread.currentThread().getName());
        return CapabilityResult.builder()
                .capabilityName(stageName)
                .output(new HashMap<>(context.getCurrentPluginOutput()))
                .data(context.getCurrentPluginOutput())
                .requestPipelineBreak(context.isPipelineBreakRequested())
                .build();
    }
}

