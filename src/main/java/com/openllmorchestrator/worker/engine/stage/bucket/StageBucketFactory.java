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
package com.openllmorchestrator.worker.engine.stage.bucket;

import com.openllmorchestrator.worker.engine.stage.custom.CustomStageBucket;
import com.openllmorchestrator.worker.engine.stage.handler.AccessStageHandler;
import com.openllmorchestrator.worker.engine.stage.handler.CustomStageHandler;
import com.openllmorchestrator.worker.engine.stage.handler.FilterStageHandler;
import com.openllmorchestrator.worker.engine.stage.handler.McpStageHandler;
import com.openllmorchestrator.worker.engine.stage.handler.MemoryStageHandler;
import com.openllmorchestrator.worker.engine.stage.handler.ModelStageHandler;
import com.openllmorchestrator.worker.engine.stage.handler.ObservabilityStageHandler;
import com.openllmorchestrator.worker.engine.stage.handler.PostProcessStageHandler;
import com.openllmorchestrator.worker.engine.stage.handler.RetrievalStageHandler;
import com.openllmorchestrator.worker.engine.stage.handler.ToolStageHandler;
import com.openllmorchestrator.worker.engine.stage.predefined.PredefinedPluginBucket;
import com.openllmorchestrator.worker.engine.stage.predefined.PredefinedStages;

/** Creates predefined and custom stage buckets. Single responsibility: bucket creation. */
public final class StageBucketFactory {
    private static final String DEFAULT_PLUGIN_ID = "default";

    private StageBucketFactory() {}

    public static PredefinedPluginBucket createPredefinedBucket() {
        return predefinedBucketBuilder().build();
    }

    public static PredefinedPluginBucket.Builder predefinedBucketBuilder() {
        return PredefinedPluginBucket.builder()
                .register(PredefinedStages.ACCESS, DEFAULT_PLUGIN_ID, new AccessStageHandler())
                .register(PredefinedStages.MEMORY, DEFAULT_PLUGIN_ID, new MemoryStageHandler())
                .register(PredefinedStages.RETRIEVAL, DEFAULT_PLUGIN_ID, new RetrievalStageHandler())
                .register(PredefinedStages.MODEL, DEFAULT_PLUGIN_ID, new ModelStageHandler())
                .register(PredefinedStages.MCP, DEFAULT_PLUGIN_ID, new McpStageHandler())
                .register(PredefinedStages.TOOL, DEFAULT_PLUGIN_ID, new ToolStageHandler())
                .register(PredefinedStages.FILTER, DEFAULT_PLUGIN_ID, new FilterStageHandler())
                .register(PredefinedStages.POST_PROCESS, DEFAULT_PLUGIN_ID, new PostProcessStageHandler())
                .register(PredefinedStages.OBSERVABILITY, DEFAULT_PLUGIN_ID, new ObservabilityStageHandler())
                .register(PredefinedStages.CUSTOM, DEFAULT_PLUGIN_ID, new CustomStageHandler());
    }

    public static CustomStageBucket createCustomBucket() {
        return CustomStageBucket.builder().build();
    }
}
