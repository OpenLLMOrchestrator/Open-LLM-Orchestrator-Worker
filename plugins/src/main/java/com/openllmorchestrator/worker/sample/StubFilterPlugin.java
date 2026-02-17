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
package com.openllmorchestrator.worker.sample;

import com.openllmorchestrator.worker.contract.ContractCompatibility;
import com.openllmorchestrator.worker.contract.PlannerInputDescriptor;
import com.openllmorchestrator.worker.contract.PluginContext;
import com.openllmorchestrator.worker.contract.PluginTypeDescriptor;
import com.openllmorchestrator.worker.contract.PluginTypes;
import com.openllmorchestrator.worker.contract.CapabilityHandler;
import com.openllmorchestrator.worker.contract.CapabilityResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Stub filter plugin: passes document through as a single tokenized chunk. For demos and contract-only plugins module. */
public final class StubFilterPlugin implements CapabilityHandler, ContractCompatibility, PlannerInputDescriptor, PluginTypeDescriptor {

    private static final String CONTRACT_VERSION = "0.0.1";
    public static final String NAME = "com.openllmorchestrator.worker.sample.StubFilterPlugin";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public CapabilityResult execute(PluginContext context) {
        Map<String, Object> input = context.getOriginalInput();
        Object doc = input.get("document");
        String text = doc != null ? doc.toString() : "";
        List<Map<String, Object>> chunks = new ArrayList<>();
        if (!text.isBlank()) {
            chunks.add(Map.of("text", text, "index", 0));
        }
        context.putOutput("tokenizedChunks", chunks);
        return CapabilityResult.builder()
                .capabilityName(NAME)
                .output(new HashMap<>(context.getCurrentPluginOutput()))
                .build();
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
        return "Filter (stub): tokenize document into chunks.";
    }

    @Override
    public String getPluginType() {
        return PluginTypes.FILTER;
    }
}
