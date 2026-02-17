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
package com.openllmorchestrator.worker.engine.capability.bucket;

import com.openllmorchestrator.worker.engine.capability.custom.CustomCapabilityBucket;
import com.openllmorchestrator.worker.engine.capability.handler.AccessCapabilityHandler;
import com.openllmorchestrator.worker.engine.capability.handler.CustomCapabilityHandler;
import com.openllmorchestrator.worker.engine.capability.handler.FilterCapabilityHandler;
import com.openllmorchestrator.worker.engine.capability.handler.McpCapabilityHandler;
import com.openllmorchestrator.worker.engine.capability.handler.MemoryCapabilityHandler;
import com.openllmorchestrator.worker.engine.capability.handler.ModelCapabilityHandler;
import com.openllmorchestrator.worker.engine.capability.handler.ObservabilityCapabilityHandler;
import com.openllmorchestrator.worker.engine.capability.handler.PlanExecutorCapabilityHandler;
import com.openllmorchestrator.worker.engine.capability.handler.PlannerCapabilityHandler;
import com.openllmorchestrator.worker.engine.capability.handler.PluginActivityHandler;
import com.openllmorchestrator.worker.engine.capability.handler.PostProcessCapabilityHandler;
import com.openllmorchestrator.worker.engine.capability.handler.RetrievalCapabilityHandler;
import com.openllmorchestrator.worker.engine.capability.handler.ToolCapabilityHandler;
import com.openllmorchestrator.worker.engine.capability.predefined.PredefinedPluginBucket;
import com.openllmorchestrator.worker.engine.capability.predefined.PredefinedCapabilities;

/** Creates predefined and custom stage buckets. Single responsibility: bucket creation. */
public final class CapabilityBucketFactory {
    private static final String DEFAULT_PLUGIN_ID = "default";

    private CapabilityBucketFactory() {}

    public static PredefinedPluginBucket createPredefinedBucket() {
        return predefinedBucketBuilder().build();
    }

    public static PredefinedPluginBucket.Builder predefinedBucketBuilder() {
        return PredefinedPluginBucket.builder()
                .register(PredefinedCapabilities.ACCESS, DEFAULT_PLUGIN_ID, new AccessCapabilityHandler())
                .register(PredefinedCapabilities.PRE_CONTEXT_SETUP, DEFAULT_PLUGIN_ID, new PluginActivityHandler(PredefinedCapabilities.PRE_CONTEXT_SETUP))
                .register(PredefinedCapabilities.PLANNER, DEFAULT_PLUGIN_ID, new PlannerCapabilityHandler())
                .register(PredefinedCapabilities.PLAN_EXECUTOR, DEFAULT_PLUGIN_ID, new PlanExecutorCapabilityHandler())
                .register(PredefinedCapabilities.EXECUTION_CONTROLLER, DEFAULT_PLUGIN_ID, new PluginActivityHandler(PredefinedCapabilities.EXECUTION_CONTROLLER))
                .register(PredefinedCapabilities.ITERATIVE_BLOCK, DEFAULT_PLUGIN_ID, new PluginActivityHandler(PredefinedCapabilities.ITERATIVE_BLOCK))
                .register(PredefinedCapabilities.MODEL, DEFAULT_PLUGIN_ID, new ModelCapabilityHandler())
                .register(PredefinedCapabilities.RETRIEVAL, DEFAULT_PLUGIN_ID, new RetrievalCapabilityHandler())
                .register(PredefinedCapabilities.RETRIEVE, DEFAULT_PLUGIN_ID, new RetrievalCapabilityHandler())
                .register(PredefinedCapabilities.TOOL, DEFAULT_PLUGIN_ID, new ToolCapabilityHandler())
                .register(PredefinedCapabilities.MCP, DEFAULT_PLUGIN_ID, new McpCapabilityHandler())
                .register(PredefinedCapabilities.MEMORY, DEFAULT_PLUGIN_ID, new MemoryCapabilityHandler())
                .register(PredefinedCapabilities.REFLECTION, DEFAULT_PLUGIN_ID, new PluginActivityHandler(PredefinedCapabilities.REFLECTION))
                .register(PredefinedCapabilities.SUB_OBSERVABILITY, DEFAULT_PLUGIN_ID, new PluginActivityHandler(PredefinedCapabilities.SUB_OBSERVABILITY))
                .register(PredefinedCapabilities.SUB_CUSTOM, DEFAULT_PLUGIN_ID, new PluginActivityHandler(PredefinedCapabilities.SUB_CUSTOM))
                .register(PredefinedCapabilities.ITERATIVE_BLOCK_END, DEFAULT_PLUGIN_ID, new PluginActivityHandler(PredefinedCapabilities.ITERATIVE_BLOCK_END))
                .register(PredefinedCapabilities.FILTER, DEFAULT_PLUGIN_ID, new FilterCapabilityHandler())
                .register(PredefinedCapabilities.POST_PROCESS, DEFAULT_PLUGIN_ID, new PostProcessCapabilityHandler())
                .register(PredefinedCapabilities.EVALUATION, DEFAULT_PLUGIN_ID, new PluginActivityHandler(PredefinedCapabilities.EVALUATION))
                .register(PredefinedCapabilities.EVALUATE, DEFAULT_PLUGIN_ID, new PluginActivityHandler(PredefinedCapabilities.EVALUATE))
                .register(PredefinedCapabilities.FEEDBACK, DEFAULT_PLUGIN_ID, new PluginActivityHandler(PredefinedCapabilities.FEEDBACK))
                .register(PredefinedCapabilities.FEEDBACK_CAPTURE, DEFAULT_PLUGIN_ID, new PluginActivityHandler(PredefinedCapabilities.FEEDBACK_CAPTURE))
                .register(PredefinedCapabilities.LEARNING, DEFAULT_PLUGIN_ID, new PluginActivityHandler(PredefinedCapabilities.LEARNING))
                .register(PredefinedCapabilities.DATASET_BUILD, DEFAULT_PLUGIN_ID, new PluginActivityHandler(PredefinedCapabilities.DATASET_BUILD))
                .register(PredefinedCapabilities.TRAIN_TRIGGER, DEFAULT_PLUGIN_ID, new PluginActivityHandler(PredefinedCapabilities.TRAIN_TRIGGER))
                .register(PredefinedCapabilities.MODEL_REGISTRY, DEFAULT_PLUGIN_ID, new PluginActivityHandler(PredefinedCapabilities.MODEL_REGISTRY))
                .register(PredefinedCapabilities.OBSERVABILITY, DEFAULT_PLUGIN_ID, new ObservabilityCapabilityHandler())
                .register(PredefinedCapabilities.CUSTOM, DEFAULT_PLUGIN_ID, new CustomCapabilityHandler());
    }

    public static CustomCapabilityBucket createCustomBucket() {
        return CustomCapabilityBucket.builder().build();
    }
}

