package com.openllmorchestrator.worker.engine.stage.handler.merge;

import com.openllmorchestrator.worker.engine.contract.AsyncGroupResultEntry;
import com.openllmorchestrator.worker.engine.contract.ExecutionContext;
import com.openllmorchestrator.worker.engine.contract.StageResult;
import com.openllmorchestrator.worker.engine.stage.StageHandler;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Merge policy plugin: prefix keys by activity name. Invoked as activity before exiting ASYNC group. */
public final class PrefixByActivityMergeHandler implements StageHandler {

    public static final String NAME = "PREFIX_BY_ACTIVITY";
    private static final String SEP = ".";

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
        @SuppressWarnings("unchecked")
        List<AsyncGroupResultEntry> results = (List<AsyncGroupResultEntry>) context.get("asyncStageResults");
        if (results == null) results = Collections.emptyList();
        for (AsyncGroupResultEntry entry : results) {
            Map<String, Object> data = entry.getResult() != null && entry.getResult().getData() != null
                    ? entry.getResult().getData() : Collections.emptyMap();
            String prefix = (entry.getActivityName() != null && !entry.getActivityName().isEmpty()
                    ? entry.getActivityName() + SEP : "");
            for (Map.Entry<String, Object> e : data.entrySet()) {
                acc.put(prefix + e.getKey(), e.getValue());
            }
        }
        return acc;
    }
}
