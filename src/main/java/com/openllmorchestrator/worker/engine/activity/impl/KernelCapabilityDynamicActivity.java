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
 * Handles capability activities invoked with a custom activity type (e.g. "RETRIEVAL::VectorStoreRetrievalPlugin")
 * so the Temporal UI shows Capability::Plugin instead of "Execute". Dispatches to the same logic as
 * {@link KernelCapabilityActivityImpl} using the capability name from the first argument.
 */
@Slf4j
public class KernelCapabilityDynamicActivity implements DynamicActivity {

    @Override
    public Object execute(EncodedValues args) {
        String activityType = Activity.getExecutionContext().getInfo().getActivityType();
        log.debug(">>> [START] Activity type: {} | Thread: {}", activityType, Thread.currentThread().getName());

        String queueName;
        String capabilityName;
        Map<String, Object> originalInput;
        Map<String, Object> accumulatedOutput;
        try {
            queueName = args.get(0, String.class);
            capabilityName = args.get(1, String.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> orig = args.get(2, Map.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> acc = args.get(3, Map.class);
            originalInput = orig != null ? orig : Map.of();
            accumulatedOutput = acc != null ? acc : Map.of();
        } catch (Exception e) {
            queueName = null;
            capabilityName = args.get(0, String.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> orig = args.get(1, Map.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> acc = args.get(2, Map.class);
            originalInput = orig != null ? orig : Map.of();
            accumulatedOutput = acc != null ? acc : Map.of();
        }

        CapabilityResolver resolver = EngineRuntime.getCapabilityResolver(queueName);
        CapabilityHandler handler = resolver.resolve(capabilityName);
        if (handler == null) {
            if (PredefinedCapabilities.isPredefined(capabilityName)) {
                throw new IllegalStateException("Predefined capability '" + capabilityName
                        + "' has no plugin registered. Register a handler for this capability.");
            }
            throw new IllegalStateException("Capability or activity '" + capabilityName
                    + "' could not be resolved. Register it in the activity registry (plugin name) or custom bucket.");
        }
        ContractVersion.requireCompatible(handler);
        ExecutionContext context = ExecutionContext.forActivity(
                originalInput != null ? originalInput : Map.of(),
                accumulatedOutput != null ? accumulatedOutput : Map.of());
        handler.execute(context);
        KernelCapabilityActivityImpl.validateOutputContract(queueName, handler, context.getCurrentPluginOutput(), capabilityName);
        log.debug("<<< [END] Activity type: {} | Thread: {}", activityType, Thread.currentThread().getName());
        return CapabilityResult.builder()
                .capabilityName(capabilityName)
                .output(new HashMap<>(context.getCurrentPluginOutput()))
                .data(context.getCurrentPluginOutput())
                .requestPipelineBreak(context.isPipelineBreakRequested())
                .build();
    }
}
