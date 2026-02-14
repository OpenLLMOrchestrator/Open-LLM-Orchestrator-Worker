package com.openllmorchestrator.worker.engine.stage.handler.merge;

import com.openllmorchestrator.worker.engine.contract.AsyncGroupResultEntry;
import com.openllmorchestrator.worker.engine.contract.ExecutionContext;
import com.openllmorchestrator.worker.engine.contract.StageResult;
import com.openllmorchestrator.worker.engine.stage.StageHandler;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Merge policy for "query all models" pipeline: concatenates each model's response as
 * response from &lt;label&gt;: "&lt;text&gt;" \n so the final output lists every model's answer.
 */
public final class AllModelsResponseFormatMergeHandler implements StageHandler {

    public static final String NAME = "ALL_MODELS_RESPONSE_FORMAT";

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
        @SuppressWarnings("unchecked")
        List<AsyncGroupResultEntry> results = context.get("asyncStageResults") instanceof List
                ? (List<AsyncGroupResultEntry>) context.get("asyncStageResults")
                : Collections.emptyList();

        StringBuilder sb = new StringBuilder();
        for (AsyncGroupResultEntry entry : results) {
            String label = getLabel(entry);
            String text = getResponseText(entry);
            sb.append("response from ").append(label).append(": \"").append(escapeQuotes(text)).append("\"\n");
        }
        String output = sb.toString();
        Map<String, Object> merged = new HashMap<>(context.getAccumulatedOutput());
        merged.put("output", output);
        merged.put("result", output);
        return merged;
    }

    private static String getLabel(AsyncGroupResultEntry entry) {
        if (entry == null || entry.getResult() == null || entry.getResult().getData() == null) {
            return entry != null && entry.getActivityName() != null ? activityNameToLabel(entry.getActivityName()) : "unknown";
        }
        Object label = entry.getResult().getData().get("modelLabel");
        if (label != null && label.toString() != null && !label.toString().isBlank()) {
            return label.toString().trim();
        }
        return activityNameToLabel(entry.getActivityName());
    }

    private static String activityNameToLabel(String activityName) {
        if (activityName == null || activityName.isBlank()) return "unknown";
        int lastDot = activityName.lastIndexOf('.');
        String className = lastDot >= 0 ? activityName.substring(lastDot + 1) : activityName;
        if (className.endsWith("ChatPlugin")) {
            className = className.substring(0, className.length() - "ChatPlugin".length());
        }
        return className.replace('_', ':');
    }

    private static String getResponseText(AsyncGroupResultEntry entry) {
        if (entry == null || entry.getResult() == null || entry.getResult().getData() == null) return "";
        Map<String, Object> data = entry.getResult().getData();
        Object result = data.get("result");
        if (result != null) return result.toString();
        Object response = data.get("response");
        if (response != null) return response.toString();
        return "";
    }

    private static String escapeQuotes(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
