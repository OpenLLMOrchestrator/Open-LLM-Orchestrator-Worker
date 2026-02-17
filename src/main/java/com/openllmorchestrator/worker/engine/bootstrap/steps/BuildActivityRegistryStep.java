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
package com.openllmorchestrator.worker.engine.bootstrap.steps;

import com.openllmorchestrator.worker.contract.CapabilityHandler;
import com.openllmorchestrator.worker.engine.bootstrap.BootstrapContext;
import com.openllmorchestrator.worker.engine.bootstrap.BootstrapStep;
import com.openllmorchestrator.worker.engine.capability.activity.ActivityRegistry;
import com.openllmorchestrator.worker.engine.capability.handler.merge.AllModelsResponseFormatMergeHandler;
import com.openllmorchestrator.worker.engine.capability.handler.merge.FirstWinsMergeHandler;
import com.openllmorchestrator.worker.engine.capability.handler.merge.LastWinsMergeHandler;
import com.openllmorchestrator.worker.engine.capability.handler.merge.PrefixByActivityMergeHandler;

/**
 * Registers in-engine handlers only (merge policies). All capability plugins (LLM, retrieval, etc.)
 * are discovered in LoadDynamicPluginsStep from the classpath (compile-time plugins in the same worker JAR)
 * and optionally from config dynamicPlugins/dynamicPluginJars (runtime JARs).
 */
public final class BuildActivityRegistryStep implements BootstrapStep {

    @Override
    public void run(BootstrapContext ctx) {
        CapabilityHandler lastWins = new LastWinsMergeHandler();
        CapabilityHandler firstWins = new FirstWinsMergeHandler();
        CapabilityHandler prefixByActivity = new PrefixByActivityMergeHandler();
        CapabilityHandler allModelsFormat = new AllModelsResponseFormatMergeHandler();

        ActivityRegistry registry = ActivityRegistry.builder()
                .register(LastWinsMergeHandler.NAME, lastWins)
                .register(FirstWinsMergeHandler.NAME, firstWins)
                .register(PrefixByActivityMergeHandler.NAME, prefixByActivity)
                .register(AllModelsResponseFormatMergeHandler.NAME, allModelsFormat)
                .build();

        ctx.setActivityRegistry(registry);
    }
}
