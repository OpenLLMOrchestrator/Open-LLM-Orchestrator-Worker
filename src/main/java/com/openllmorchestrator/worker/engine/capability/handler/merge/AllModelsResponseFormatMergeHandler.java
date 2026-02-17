/*
 * Copyright 2026 Open LLM Orchestrator contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.openllmorchestrator.worker.engine.capability.handler.merge;

import com.openllmorchestrator.worker.engine.contract.AsyncGroupResultEntry;
import com.openllmorchestrator.worker.contract.PluginContext;
import com.openllmorchestrator.worker.contract.CapabilityHandler;
import com.openllmorchestrator.worker.contract.CapabilityResult;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Merge policy for "query all models" pipeline: concatenates each model's response as
 * response from &lt;label&gt;: "&lt;text&gt;" \n so the final output lists every model's answer.
 */
public final class AllModelsResponseFormatMergeHandler implements CapabilityHandler {

    public static final String NAME = "ALL_MODELS_RESPONSE_FORMAT";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public CapabilityResult execute(PluginContext context) {
        Map<String, Object> merged = merge(context);
        for (Map.Entry<String, Object> e : merged.entrySet()) {
            context.putOutput(e.getKey(), e.getValue());
        }
        return CapabilityResult.builder().capabilityName(NAME).data(merged).build();
    }

    private static Map<String, Object> merge(PluginContext context) {
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


