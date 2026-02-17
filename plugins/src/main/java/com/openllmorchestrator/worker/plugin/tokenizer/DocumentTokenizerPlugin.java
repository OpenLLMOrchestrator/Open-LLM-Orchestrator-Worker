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
package com.openllmorchestrator.worker.plugin.tokenizer;

import com.openllmorchestrator.worker.contract.ContractCompatibility;
import com.openllmorchestrator.worker.contract.PluginContext;
import com.openllmorchestrator.worker.contract.PlannerInputDescriptor;
import com.openllmorchestrator.worker.contract.PluginTypeDescriptor;
import com.openllmorchestrator.worker.contract.PluginTypes;
import com.openllmorchestrator.worker.contract.CapabilityHandler;
import com.openllmorchestrator.worker.contract.CapabilityResult;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Document tokenizer plugin. Splits document content into chunks (e.g. for embedding and storage).
 */
public final class DocumentTokenizerPlugin implements CapabilityHandler, ContractCompatibility, PlannerInputDescriptor, PluginTypeDescriptor {

    private static final String CONTRACT_VERSION = "0.0.1";
    public static final String NAME = "com.openllmorchestrator.worker.plugin.tokenizer.DocumentTokenizerPlugin";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public CapabilityResult execute(PluginContext context) {
        Map<String, Object> input = context.getOriginalInput();

        Object docObj = input.get("document");
        String content = docObj != null ? docObj.toString() : "";
        List<Map<String, Object>> chunks = tokenize(content);

        context.putOutput("tokenizedChunks", chunks);

        return CapabilityResult.builder().capabilityName(NAME).data(context.getCurrentPluginOutput()).build();
    }

    @Override
    public String getRequiredContractVersion() {
        return CONTRACT_VERSION;
    }

    @Override
    public Set<String> getRequiredInputFieldsForPlanner() {
        return Set.of("document");
    }

    @Override
    public String getPlannerDescription() {
        return "Filter: tokenize document into chunks for storage.";
    }

    @Override
    public String getPluginType() {
        return PluginTypes.FILTER;
    }

    private List<Map<String, Object>> tokenize(String content) {
        // Stub: in real impl, split by sentences/paragraphs, optional tokenization, return chunk maps.
        if (content == null || content.isBlank()) {
            return List.of();
        }
        return List.of(Map.<String, Object>of("text", content, "index", 0));
    }
}


