package com.openllmorchestrator.worker.engine.activity;

import com.openllmorchestrator.worker.engine.contract.StageResult;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

import java.util.Map;

@ActivityInterface
public interface KernelStageActivity {

    /**
     * Execute a stage with pipeline data context. Every plugin receives original input and
     * accumulated output from previous stages; returns its output in result.data.
     */
    @ActivityMethod
    StageResult execute(String stageName, Map<String, Object> originalInput, Map<String, Object> accumulatedOutput);
}
