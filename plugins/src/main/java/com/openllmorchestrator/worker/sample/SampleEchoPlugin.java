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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Sample plugin that depends only on plugin-contract.
 * Echoes original input into output. Implements {@link ContractCompatibility},
 * {@link PlannerInputDescriptor} and {@link PluginTypeDescriptor} so it can be
 * sent as an "available tool" when the planner filters by type.
 */
public final class SampleEchoPlugin implements CapabilityHandler, ContractCompatibility, PlannerInputDescriptor, PluginTypeDescriptor {

    /** Contract version this plugin was built against (match plugin-contract dependency version). */
    private static final String CONTRACT_VERSION = "0.0.1";

    public static final String NAME = "com.openllmorchestrator.worker.sample.SampleEchoPlugin";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public CapabilityResult execute(PluginContext context) {
        Map<String, Object> out = new HashMap<>(context.getOriginalInput());
        out.put("_echo", true);
        for (Map.Entry<String, Object> e : out.entrySet()) {
            context.putOutput(e.getKey(), e.getValue());
        }
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
        return Set.of("question", "document", "messages");
    }

    @Override
    public String getPlannerDescription() {
        return "Echo plugin: forwards original input to output.";
    }

    @Override
    public String getPluginType() {
        return PluginTypes.TOOL;
    }
}
