package com.openllmorchestrator.worker.engine.stage.handler;

import com.openllmorchestrator.worker.engine.contract.ExecutionContext;
import com.openllmorchestrator.worker.engine.contract.StageResult;
import com.openllmorchestrator.worker.engine.stage.StageHandler;

/** Default OBSERVABILITY stage handler. */
public final class ObservabilityStageHandler implements StageHandler {
    public static final String NAME = "OBSERVABILITY";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public StageResult execute(ExecutionContext context) {
        return StageResult.builder().stageName(NAME).build();
    }
}
