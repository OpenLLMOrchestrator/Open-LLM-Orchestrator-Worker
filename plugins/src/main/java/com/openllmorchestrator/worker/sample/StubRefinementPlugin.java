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
import com.openllmorchestrator.worker.contract.StageHandler;
import com.openllmorchestrator.worker.contract.StageResult;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** Stub refinement plugin: formats result/response as ANS: "...". For demos and contract-only plugins module. */
public final class StubRefinementPlugin implements StageHandler, ContractCompatibility, PlannerInputDescriptor, PluginTypeDescriptor {

    private static final String CONTRACT_VERSION = "0.0.1";
    public static final String NAME = "com.openllmorchestrator.worker.sample.StubRefinementPlugin";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public StageResult execute(PluginContext context) {
        Map<String, Object> accumulated = context.getAccumulatedOutput();
        Object result = accumulated.get("result");
        if (result == null) {
            result = accumulated.get("response");
        }
        String text = result != null ? result.toString().trim() : "";
        String formatted = "ANS: \"" + (text.isEmpty() ? "" : text.replace("\\", "\\\\").replace("\"", "\\\"")) + "\"";
        context.putOutput("output", formatted);
        return StageResult.builder()
                .stageName(NAME)
                .output(new HashMap<>(context.getCurrentPluginOutput()))
                .build();
    }

    @Override
    public String getRequiredContractVersion() {
        return CONTRACT_VERSION;
    }

    @Override
    public Set<String> getRequiredInputFieldsForPlanner() {
        return Set.of("result", "response");
    }

    @Override
    public String getPlannerDescription() {
        return "Refinement (stub): format result as ANS: \"...\".";
    }

    @Override
    public String getPluginType() {
        return PluginTypes.REFINEMENT;
    }
}
