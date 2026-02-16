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
package com.openllmorchestrator.worker.plugin.guardrail;

import com.openllmorchestrator.worker.contract.ContractCompatibility;
import com.openllmorchestrator.worker.contract.PluginContext;
import com.openllmorchestrator.worker.contract.PlannerInputDescriptor;
import com.openllmorchestrator.worker.contract.PluginTypeDescriptor;
import com.openllmorchestrator.worker.contract.PluginTypes;
import com.openllmorchestrator.worker.contract.StageHandler;
import com.openllmorchestrator.worker.contract.StageResult;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * GUARDRAIL plugin: optional max length and blocklist. Reads question or content from input/accumulated;
 * if length exceeds maxLength (input.maxLength or 10000) or contains blocklist term (input.blocklistWords comma-separated),
 * sets guardrailTriggered and filteredContent.
 */
public final class SimpleGuardrailPlugin implements StageHandler, ContractCompatibility, PlannerInputDescriptor, PluginTypeDescriptor {

    private static final String CONTRACT_VERSION = "0.0.1";
    public static final String NAME = "com.openllmorchestrator.worker.plugin.guardrail.SimpleGuardrailPlugin";
    private static final int DEFAULT_MAX_LENGTH = 10000;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public StageResult execute(PluginContext context) {
        Map<String, Object> input = context.getOriginalInput();
        Map<String, Object> accumulated = context.getAccumulatedOutput();
        String content = null;
        if (input != null && input.get("question") != null) {
            content = String.valueOf(input.get("question"));
        }
        if (content == null && accumulated != null && accumulated.get("result") != null) {
            content = String.valueOf(accumulated.get("result"));
        }
        if (content == null) {
            content = "";
        }
        int maxLen = input != null && input.get("maxLength") instanceof Number
                ? ((Number) input.get("maxLength")).intValue() : DEFAULT_MAX_LENGTH;
        if (maxLen <= 0) {
            maxLen = DEFAULT_MAX_LENGTH;
        }
        String blocklistStr = input != null ? (String) input.get("blocklistWords") : null;
        boolean triggered = false;
        String filtered = content;
        if (content.length() > maxLen) {
            triggered = true;
            filtered = content.substring(0, maxLen);
        }
        if (blocklistStr != null && !blocklistStr.isBlank()) {
            String lower = content.toLowerCase();
            for (String word : blocklistStr.split(",")) {
                String w = word.trim().toLowerCase();
                if (!w.isEmpty() && lower.contains(w)) {
                    triggered = true;
                    filtered = filtered.replaceAll("(?i)" + java.util.regex.Pattern.quote(word.trim()), "[REDACTED]");
                }
            }
        }
        context.putOutput("guardrailTriggered", triggered);
        context.putOutput("filteredContent", filtered);
        return StageResult.builder().stageName(NAME).data(new HashMap<>(context.getCurrentPluginOutput())).build();
    }

    @Override
    public String getRequiredContractVersion() {
        return CONTRACT_VERSION;
    }

    @Override
    public Set<String> getRequiredInputFieldsForPlanner() {
        return Set.of("question", "result", "maxLength", "blocklistWords");
    }

    @Override
    public String getPlannerDescription() {
        return "Guardrail: max length and optional blocklist; sets guardrailTriggered and filteredContent.";
    }

    @Override
    public String getPluginType() {
        return PluginTypes.GUARDRAIL;
    }
}
