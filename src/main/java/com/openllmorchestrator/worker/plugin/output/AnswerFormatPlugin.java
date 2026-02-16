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
package com.openllmorchestrator.worker.plugin.output;

import com.openllmorchestrator.worker.contract.PluginContext;
import com.openllmorchestrator.worker.contract.StageHandler;
import com.openllmorchestrator.worker.contract.StageResult;

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
    public StageResult execute(PluginContext context) {
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


