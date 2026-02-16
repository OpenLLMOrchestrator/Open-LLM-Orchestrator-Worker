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
import com.openllmorchestrator.worker.engine.stage.handler.PlanExecutorStageHandler;
import com.openllmorchestrator.worker.engine.stage.handler.PlannerStageHandler;
import com.openllmorchestrator.worker.engine.stage.handler.PluginActivityHandler;
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
                .register(PredefinedStages.PRE_CONTEXT_SETUP, DEFAULT_PLUGIN_ID, new PluginActivityHandler(PredefinedStages.PRE_CONTEXT_SETUP))
                .register(PredefinedStages.PLANNER, DEFAULT_PLUGIN_ID, new PlannerStageHandler())
                .register(PredefinedStages.PLAN_EXECUTOR, DEFAULT_PLUGIN_ID, new PlanExecutorStageHandler())
                .register(PredefinedStages.EXECUTION_CONTROLLER, DEFAULT_PLUGIN_ID, new PluginActivityHandler(PredefinedStages.EXECUTION_CONTROLLER))
                .register(PredefinedStages.ITERATIVE_BLOCK, DEFAULT_PLUGIN_ID, new PluginActivityHandler(PredefinedStages.ITERATIVE_BLOCK))
                .register(PredefinedStages.MODEL, DEFAULT_PLUGIN_ID, new ModelStageHandler())
                .register(PredefinedStages.RETRIEVAL, DEFAULT_PLUGIN_ID, new RetrievalStageHandler())
                .register(PredefinedStages.RETRIEVE, DEFAULT_PLUGIN_ID, new RetrievalStageHandler())
                .register(PredefinedStages.TOOL, DEFAULT_PLUGIN_ID, new ToolStageHandler())
                .register(PredefinedStages.MCP, DEFAULT_PLUGIN_ID, new McpStageHandler())
                .register(PredefinedStages.MEMORY, DEFAULT_PLUGIN_ID, new MemoryStageHandler())
                .register(PredefinedStages.REFLECTION, DEFAULT_PLUGIN_ID, new PluginActivityHandler(PredefinedStages.REFLECTION))
                .register(PredefinedStages.SUB_OBSERVABILITY, DEFAULT_PLUGIN_ID, new PluginActivityHandler(PredefinedStages.SUB_OBSERVABILITY))
                .register(PredefinedStages.SUB_CUSTOM, DEFAULT_PLUGIN_ID, new PluginActivityHandler(PredefinedStages.SUB_CUSTOM))
                .register(PredefinedStages.ITERATIVE_BLOCK_END, DEFAULT_PLUGIN_ID, new PluginActivityHandler(PredefinedStages.ITERATIVE_BLOCK_END))
                .register(PredefinedStages.FILTER, DEFAULT_PLUGIN_ID, new FilterStageHandler())
                .register(PredefinedStages.POST_PROCESS, DEFAULT_PLUGIN_ID, new PostProcessStageHandler())
                .register(PredefinedStages.EVALUATION, DEFAULT_PLUGIN_ID, new PluginActivityHandler(PredefinedStages.EVALUATION))
                .register(PredefinedStages.EVALUATE, DEFAULT_PLUGIN_ID, new PluginActivityHandler(PredefinedStages.EVALUATE))
                .register(PredefinedStages.FEEDBACK, DEFAULT_PLUGIN_ID, new PluginActivityHandler(PredefinedStages.FEEDBACK))
                .register(PredefinedStages.FEEDBACK_CAPTURE, DEFAULT_PLUGIN_ID, new PluginActivityHandler(PredefinedStages.FEEDBACK_CAPTURE))
                .register(PredefinedStages.LEARNING, DEFAULT_PLUGIN_ID, new PluginActivityHandler(PredefinedStages.LEARNING))
                .register(PredefinedStages.DATASET_BUILD, DEFAULT_PLUGIN_ID, new PluginActivityHandler(PredefinedStages.DATASET_BUILD))
                .register(PredefinedStages.TRAIN_TRIGGER, DEFAULT_PLUGIN_ID, new PluginActivityHandler(PredefinedStages.TRAIN_TRIGGER))
                .register(PredefinedStages.MODEL_REGISTRY, DEFAULT_PLUGIN_ID, new PluginActivityHandler(PredefinedStages.MODEL_REGISTRY))
                .register(PredefinedStages.OBSERVABILITY, DEFAULT_PLUGIN_ID, new ObservabilityStageHandler())
                .register(PredefinedStages.CUSTOM, DEFAULT_PLUGIN_ID, new CustomStageHandler());
    }

    public static CustomStageBucket createCustomBucket() {
        return CustomStageBucket.builder().build();
    }
}

