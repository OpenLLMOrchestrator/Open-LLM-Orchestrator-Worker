package com.openllmorchestrator.worker.plugin.vectordb;

import com.openllmorchestrator.worker.engine.contract.ExecutionContext;
import com.openllmorchestrator.worker.engine.contract.StageResult;
import com.openllmorchestrator.worker.engine.stage.StageHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Vector DB plugin: store chunks (doc pipeline) or retrieve (question pipeline). */
public final class VectorStoreRetrievalPlugin implements StageHandler {

    public static final String NAME = "VectorStoreRetrievalPlugin";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public StageResult execute(ExecutionContext context) {
        Map<String, Object> accumulated = context.getAccumulatedOutput();
        Map<String, Object> input = context.getOriginalInput();

        Object chunksObj = accumulated.get("tokenizedChunks");
        if (chunksObj instanceof List && !((List<?>) chunksObj).isEmpty()) {
            context.putOutput("stored", true);
            context.putOutput("chunkCount", ((List<?>) chunksObj).size());
            return StageResult.builder().stageName(NAME).data(new HashMap<>(context.getCurrentPluginOutput())).build();
        }

        String question = (String) input.get("question");
        if (question != null && !question.isBlank()) {
            context.putOutput("retrievedChunks", retrieveFromVectorDb(question));
        }

        return StageResult.builder().stageName(NAME).data(new HashMap<>(context.getCurrentPluginOutput())).build();
    }

    private List<Map<String, Object>> retrieveFromVectorDb(String question) {
        return new ArrayList<>();
    }
}
