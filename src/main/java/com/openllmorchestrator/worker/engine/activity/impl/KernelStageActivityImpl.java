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

import com.openllmorchestrator.worker.engine.activity.KernelStageActivity;
import com.openllmorchestrator.worker.engine.config.FeatureFlag;
import com.openllmorchestrator.worker.engine.contract.ExecutionContext;
import com.openllmorchestrator.worker.contract.StageResult;
import com.openllmorchestrator.worker.engine.runtime.EngineRuntime;
import com.openllmorchestrator.worker.contract.OutputContract;
import com.openllmorchestrator.worker.contract.OutputContractValidator;
import com.openllmorchestrator.worker.contract.ContractVersion;
import com.openllmorchestrator.worker.contract.OutputContractViolationException;
import com.openllmorchestrator.worker.contract.StageHandler;
import com.openllmorchestrator.worker.engine.stage.predefined.PredefinedStages;
import com.openllmorchestrator.worker.engine.stage.resolver.StageResolver;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * Resolves stage by name; runs handler with original input and accumulated output;
 * returns result with current plugin output in data.
 */
@Slf4j
public class KernelStageActivityImpl implements KernelStageActivity {

    @Override
    public StageResult execute(String stageName, Map<String, Object> originalInput, Map<String, Object> accumulatedOutput) {
        log.debug(">>> [START] Stage: {} | Thread: {}", stageName, Thread.currentThread().getName());

        StageResolver resolver = EngineRuntime.getStageResolver();
        StageHandler handler = resolver.resolve(stageName);
        if (handler == null) {
            if (PredefinedStages.isPredefined(stageName)) {
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
        StageResult handlerResult = handler.execute(context);
        validateOutputContract(handler, context.getCurrentPluginOutput(), stageName);
        log.debug("<<< [END] Stage: {} | Thread: {}", stageName, Thread.currentThread().getName());
        Map<String, Object> output = context.getCurrentPluginOutput() != null && !context.getCurrentPluginOutput().isEmpty()
                ? new HashMap<>(context.getCurrentPluginOutput())
                : (handlerResult != null && handlerResult.getOutput() != null ? new HashMap<>(handlerResult.getOutput()) : new HashMap<>());
        boolean requestBreak = context.isPipelineBreakRequested() || (handlerResult != null && handlerResult.isRequestPipelineBreak());
        return StageResult.builder()
                .stageName(stageName)
                .output(output)
                .requestPipelineBreak(requestBreak)
                .build();
    }

    static void validateOutputContract(StageHandler handler, Map<String, Object> output, String stageName) {
        if (EngineRuntime.getFeatureFlags() != null && !EngineRuntime.getFeatureFlags().isEnabled(FeatureFlag.OUTPUT_CONTRACT)) {
            return;
        }
        if (!(handler instanceof OutputContract contract)) {
            return;
        }
        OutputContractValidator validator = EngineRuntime.getOutputContractValidator();
        if (validator == null) {
            return;
        }
        Object schema = contract.getSchema();
        if (schema == null) {
            return;
        }
        Map<String, Object> out = output != null ? output : Map.of();
        if (validator.validate(out, schema)) {
            return;
        }
        if (contract.enforceStrict()) {
            throw new OutputContractViolationException(
                    "Stage '" + stageName + "' output did not satisfy OutputContract schema (enforceStrict=true).");
        }
        log.warn("Stage '{}' output did not satisfy OutputContract schema (enforceStrict=false).", stageName);
    }
}

