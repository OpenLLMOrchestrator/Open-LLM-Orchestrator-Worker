package com.openllmorchestrator.worker.engine.stage.handler;

import com.openllmorchestrator.worker.engine.contract.ExecutionContext;
import com.openllmorchestrator.worker.engine.contract.StageResult;
import com.openllmorchestrator.worker.engine.stage.StageHandler;

/**
 * StageHandler that represents one plugin/activity by name.
 * Each activity name (e.g. AccessControlPlugin) is implemented by one such handler.
 */
public final class PluginActivityHandler implements StageHandler {
    private final String activityName;

    public PluginActivityHandler(String activityName) {
        if (activityName == null || activityName.isBlank()) {
            throw new IllegalArgumentException("activityName must be non-blank");
        }
        this.activityName = activityName;
    }

    @Override
    public String name() {
        return activityName;
    }

    @Override
    public StageResult execute(ExecutionContext context) {
        return StageResult.builder().stageName(activityName).build();
    }
}
