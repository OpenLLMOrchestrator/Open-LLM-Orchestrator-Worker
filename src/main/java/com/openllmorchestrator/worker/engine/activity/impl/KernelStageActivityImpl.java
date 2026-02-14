package com.openllmorchestrator.worker.engine.activity.impl;

import com.openllmorchestrator.worker.engine.activity.KernelStageActivity;
import com.openllmorchestrator.worker.engine.contract.ExecutionContext;
import com.openllmorchestrator.worker.engine.contract.StageResult;
import com.openllmorchestrator.worker.engine.runtime.EngineRuntime;
import com.openllmorchestrator.worker.engine.stage.StageHandler;
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
        ExecutionContext context = ExecutionContext.forActivity(
                originalInput != null ? originalInput : Map.of(),
                accumulatedOutput != null ? accumulatedOutput : Map.of());
        handler.execute(context);
        log.debug("<<< [END] Stage: {} | Thread: {}", stageName, Thread.currentThread().getName());
        return StageResult.builder()
                .stageName(stageName)
                .data(new HashMap<>(context.getCurrentPluginOutput()))
                .build();
    }
}
