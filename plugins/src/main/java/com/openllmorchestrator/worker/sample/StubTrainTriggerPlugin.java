/*
 * Copyright 2026 Open LLM Orchestrator contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use it except in compliance with the License.
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
import com.openllmorchestrator.worker.contract.PluginTypeDescriptor;
import com.openllmorchestrator.worker.contract.PluginTypes;
import com.openllmorchestrator.worker.contract.CapabilityHandler;
import com.openllmorchestrator.worker.contract.CapabilityResult;

import java.util.Map;

/** Stub train-trigger plugin: no-op placeholder for TRAIN_TRIGGER stage. Replace with a real implementation to trigger fine-tuning or training jobs when conditions are met. */
public final class StubTrainTriggerPlugin implements CapabilityHandler, ContractCompatibility, PluginTypeDescriptor {

    private static final String CONTRACT_VERSION = "0.0.1";
    public static final String NAME = "com.openllmorchestrator.worker.sample.StubTrainTriggerPlugin";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public CapabilityResult execute(PluginContext context) {
        context.putOutput("trainTriggerRun", false);
        context.putOutput("trainTriggerStub", true);
        return CapabilityResult.builder()
                .capabilityName(NAME)
                .output(Map.copyOf(context.getCurrentPluginOutput()))
                .build();
    }

    @Override
    public String getRequiredContractVersion() {
        return CONTRACT_VERSION;
    }

    @Override
    public String getPluginType() {
        return PluginTypes.TRAIN_TRIGGER;
    }
}
