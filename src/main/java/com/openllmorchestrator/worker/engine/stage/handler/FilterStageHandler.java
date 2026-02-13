package com.openllmorchestrator.worker.engine.stage.handler;

import com.openllmorchestrator.worker.engine.contract.ExecutionContext;
import com.openllmorchestrator.worker.engine.contract.StageResult;
import com.openllmorchestrator.worker.engine.stage.StageHandler;

/** Default FILTER stage handler. */
public final class FilterStageHandler implements StageHandler {
    public static final String NAME = "FILTER";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public StageResult execute(ExecutionContext context) {
        return StageResult.builder().stageName(NAME).build();
    }
}
