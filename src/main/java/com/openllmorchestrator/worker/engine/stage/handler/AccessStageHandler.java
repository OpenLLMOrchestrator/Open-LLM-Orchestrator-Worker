package com.openllmorchestrator.worker.engine.stage.handler;

import com.openllmorchestrator.worker.engine.contract.ExecutionContext;
import com.openllmorchestrator.worker.engine.contract.StageResult;
import com.openllmorchestrator.worker.engine.stage.StageHandler;

/** Default ACCESS stage handler. Reads originalInput/accumulatedOutput; writes output via putOutput. */
public final class AccessStageHandler implements StageHandler {
    public static final String NAME = "ACCESS";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public StageResult execute(ExecutionContext context) {
        // Plugins can read: context.getOriginalInput(), context.getAccumulatedOutput()
        // and write: context.putOutput(key, value) for consumption by next stages
        return StageResult.builder().stageName(NAME).build();
    }
}
