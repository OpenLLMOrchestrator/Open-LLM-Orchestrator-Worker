package com.openllmorchestrator.worker.engine.stage.handler.merge;

import com.openllmorchestrator.worker.engine.contract.AsyncGroupResultEntry;
import com.openllmorchestrator.worker.engine.contract.ExecutionContext;
import com.openllmorchestrator.worker.engine.contract.StageResult;
import com.openllmorchestrator.worker.engine.stage.StageHandler;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Merge policy plugin: first writer wins (putIfAbsent). Invoked as activity before exiting ASYNC group. */
public final class FirstWinsMergeHandler implements StageHandler {

    public static final String NAME = "FIRST_WINS";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public StageResult execute(ExecutionContext context) {
        Map<String, Object> merged = merge(context);
        for (Map.Entry<String, Object> e : merged.entrySet()) {
            context.putOutput(e.getKey(), e.getValue());
        }
        return StageResult.builder().stageName(NAME).data(merged).build();
    }

    private static Map<String, Object> merge(ExecutionContext context) {
        Map<String, Object> acc = new HashMap<>(context.getAccumulatedOutput());
        List<AsyncGroupResultEntry> results = getResults(context);
        for (AsyncGroupResultEntry entry : results) {
            Map<String, Object> data = entry.getResult() != null && entry.getResult().getData() != null
                    ? entry.getResult().getData() : Collections.emptyMap();
            for (Map.Entry<String, Object> e : data.entrySet()) {
                acc.putIfAbsent(e.getKey(), e.getValue());
            }
        }
        return acc;
    }

    @SuppressWarnings("unchecked")
    private static List<AsyncGroupResultEntry> getResults(ExecutionContext context) {
        Object o = context.get("asyncStageResults");
        return o instanceof List ? (List<AsyncGroupResultEntry>) o : Collections.emptyList();
    }
}
