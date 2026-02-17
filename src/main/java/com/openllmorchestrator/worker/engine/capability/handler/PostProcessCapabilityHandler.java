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
package com.openllmorchestrator.worker.engine.capability.handler;

import com.openllmorchestrator.worker.contract.PluginContext;
import com.openllmorchestrator.worker.contract.CapabilityHandler;
import com.openllmorchestrator.worker.contract.CapabilityResult;

/** Default POST_PROCESS stage handler. */
public final class PostProcessCapabilityHandler implements CapabilityHandler {
    public static final String NAME = "POST_PROCESS";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public CapabilityResult execute(PluginContext context) {
        return CapabilityResult.builder().capabilityName(NAME).build();
    }
}


