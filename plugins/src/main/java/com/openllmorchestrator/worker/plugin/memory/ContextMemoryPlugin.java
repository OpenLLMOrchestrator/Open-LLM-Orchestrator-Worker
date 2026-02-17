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
package com.openllmorchestrator.worker.plugin.memory;

import com.openllmorchestrator.worker.contract.ContractCompatibility;
import com.openllmorchestrator.worker.contract.PluginContext;
import com.openllmorchestrator.worker.contract.PlannerInputDescriptor;
import com.openllmorchestrator.worker.contract.PluginTypeDescriptor;
import com.openllmorchestrator.worker.contract.PluginTypes;
import com.openllmorchestrator.worker.contract.CapabilityHandler;
import com.openllmorchestrator.worker.contract.CapabilityResult;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * MEMORY stage plugin: reads/writes key-value via context state (put/get). Request-scoped.
 */
public final class ContextMemoryPlugin implements CapabilityHandler, ContractCompatibility, PlannerInputDescriptor, PluginTypeDescriptor {

    private static final String CONTRACT_VERSION = "0.0.1";
    public static final String NAME = "com.openllmorchestrator.worker.plugin.memory.ContextMemoryPlugin";
    private static final String STATE_PREFIX = "memory:";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public CapabilityResult execute(PluginContext context) {
        Map<String, Object> input = context.getOriginalInput();
        String key = input != null ? (String) input.get("memoryKey") : null;
        Object valueToWrite = input != null ? input.get("memoryValue") : null;
        if (key != null && !key.isBlank()) {
            String stateKey = STATE_PREFIX + key;
            if (valueToWrite != null) {
                context.put(stateKey, valueToWrite);
                context.putOutput("memoryValue", valueToWrite);
                context.putOutput("written", true);
            } else {
                Object read = context.get(stateKey);
                context.putOutput("memoryValue", read);
            }
        }
        return CapabilityResult.builder().capabilityName(NAME).data(new HashMap<>(context.getCurrentPluginOutput())).build();
    }

    @Override
    public String getRequiredContractVersion() {
        return CONTRACT_VERSION;
    }

    @Override
    public Set<String> getRequiredInputFieldsForPlanner() {
        return Set.of("memoryKey", "memoryValue");
    }

    @Override
    public String getPlannerDescription() {
        return "Memory: read/write key-value via context state (request-scoped).";
    }

    @Override
    public String getPluginType() {
        return PluginTypes.MEMORY;
    }
}
