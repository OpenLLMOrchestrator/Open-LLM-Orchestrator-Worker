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
import com.openllmorchestrator.worker.contract.PluginContext;
import com.openllmorchestrator.worker.contract.StageHandler;
import com.openllmorchestrator.worker.contract.StageResult;

import java.util.HashMap;
import java.util.Map;

/**
 * Sample plugin that depends only on plugin-contract.
 * Echoes original input into output. Implements {@link ContractCompatibility} to declare
 * the contract version this plugin was built for (for runtime compatibility check).
 */
public final class SampleEchoPlugin implements StageHandler, ContractCompatibility {

    /** Contract version this plugin was built against (match plugin-contract dependency version). */
    private static final String CONTRACT_VERSION = "0.0.1";

    public static final String NAME = "com.openllmorchestrator.worker.sample.SampleEchoPlugin";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public StageResult execute(PluginContext context) {
        Map<String, Object> out = new HashMap<>(context.getOriginalInput());
        out.put("_echo", true);
        for (Map.Entry<String, Object> e : out.entrySet()) {
            context.putOutput(e.getKey(), e.getValue());
        }
        return StageResult.builder()
                .stageName(NAME)
                .output(new HashMap<>(context.getCurrentPluginOutput()))
                .build();
    }

    @Override
    public String getRequiredContractVersion() {
        return CONTRACT_VERSION;
    }
}
