package com.openllmorchestrator.worker.plugin.output;

import com.openllmorchestrator.worker.engine.contract.ExecutionContext;
import com.openllmorchestrator.worker.engine.contract.StageResult;
import com.openllmorchestrator.worker.engine.stage.StageHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * Post-process stage that renders the model output as a single line: ANS: "&lt;response&gt;".
 * Reads "result" or "response" from accumulated output and writes "output" in that format.
 */
public final class AnswerFormatPlugin implements StageHandler {

    public static final String NAME = "AnswerFormatPlugin";
    private static final String PREFIX = "ANS: \"";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public StageResult execute(ExecutionContext context) {
        Map<String, Object> accumulated = context.getAccumulatedOutput();
        String text = null;
        Object result = accumulated.get("result");
        if (result != null && result.toString() != null) {
            text = result.toString().trim();
        }
        if (text == null || text.isEmpty()) {
            Object response = accumulated.get("response");
            if (response != null && response.toString() != null) {
                text = response.toString().trim();
            }
        }
        if (text == null) {
            text = "";
        }
        String formatted = PREFIX + escapeQuotes(text) + "\"";
        context.putOutput("output", formatted);
        return StageResult.builder().stageName(NAME).data(new HashMap<>(context.getCurrentPluginOutput())).build();
    }

    private static String escapeQuotes(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
