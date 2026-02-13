package com.openllmorchestrator.worker.engine.stage;

import java.util.List;

public class StageRegistry {

    private final List<StageDefinition> definitions;

    public StageRegistry(List<StageDefinition> definitions) {
        this.definitions = definitions;
    }

    public StagePlan buildPlan() {
        StagePlan.Builder builder = StagePlan.builder();
        builder.addSync("ACCESS");
        builder.addSync("MEMORY");
        builder.addSync("RETRIEVAL");
        builder.addSync("MODEL");
        return builder.build();
    }
}
