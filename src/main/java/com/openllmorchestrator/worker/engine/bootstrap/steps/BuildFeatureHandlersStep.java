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

import com.openllmorchestrator.worker.contract.FeatureExecutionPlugin;
import com.openllmorchestrator.worker.engine.bootstrap.BootstrapContext;
import com.openllmorchestrator.worker.engine.bootstrap.BootstrapStep;
import com.openllmorchestrator.worker.engine.config.EngineConfigRuntime;
import com.openllmorchestrator.worker.engine.config.EngineFileConfig;
import com.openllmorchestrator.worker.engine.config.FeatureFlag;
import com.openllmorchestrator.worker.engine.kernel.feature.FeatureExecutionPluginRegistry;
import com.openllmorchestrator.worker.engine.kernel.feature.FeatureHandler;
import com.openllmorchestrator.worker.engine.kernel.feature.FeatureHandlerRegistry;
import com.openllmorchestrator.worker.engine.kernel.feature.FeatureHandlerRegistryBuilder;
import com.openllmorchestrator.worker.engine.kernel.feature.FeatureHandlerFactory;
import com.openllmorchestrator.worker.engine.kernel.feature.DefaultFeatureHandlerFactory;
import com.openllmorchestrator.worker.engine.kernel.feature.FeatureHandlerWithPlugins;
import com.openllmorchestrator.worker.engine.kernel.interceptor.ExecutionInterceptorChain;
import com.openllmorchestrator.worker.engine.runtime.EngineRuntime;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds feature handler registry (map + ordered list) and execution interceptor chain at bootstrap.
 * Loads feature execution plugins via ServiceLoader; resolves per-feature plugins from config (featurePlugins)
 * and attaches them to each feature handler so pre/post run at every node with context data.
 */
public final class BuildFeatureHandlersStep implements BootstrapStep {

    private final FeatureHandlerFactory factory;

    public BuildFeatureHandlersStep() {
        this(DefaultFeatureHandlerFactory.getInstance());
    }

    public BuildFeatureHandlersStep(FeatureHandlerFactory factory) {
        this.factory = factory != null ? factory : DefaultFeatureHandlerFactory.getInstance();
    }

    @Override
    public void run(BootstrapContext ctx) {
        String queueName = ctx.getQueueName() != null && !ctx.getQueueName().isBlank() ? ctx.getQueueName() : "default";

        FeatureExecutionPluginRegistry pluginRegistry = FeatureExecutionPluginRegistry.loadFromServiceLoader();
        EngineRuntime.setFeatureExecutionPluginRegistry(queueName, pluginRegistry);

        var featureFlags = EngineConfigRuntime.getFeatureFlagsEffective(ctx.getConfig());
        FeatureHandlerRegistry registry = FeatureHandlerRegistryBuilder.build(featureFlags, factory);

        EngineFileConfig config = ctx.getConfig();
        Map<String, List<String>> featurePluginsConfig = config != null ? config.getFeaturePluginsEffective() : Map.of();

        Map<FeatureFlag, FeatureHandler> wrappedMap = new LinkedHashMap<>();
        List<FeatureHandler> wrappedOrdered = new ArrayList<>();
        for (FeatureHandler handler : registry.getOrderedHandlers()) {
            String featureName = handler.getFeature().name();
            List<String> pluginNames = featurePluginsConfig.getOrDefault(featureName, List.of());
            List<FeatureExecutionPlugin> plugins = new ArrayList<>();
            for (String name : pluginNames) {
                FeatureExecutionPlugin plugin = pluginRegistry.get(name);
                if (plugin != null) {
                    plugins.add(plugin);
                }
            }
            FeatureHandler target = plugins.isEmpty() ? handler : new FeatureHandlerWithPlugins(handler, plugins);
            wrappedMap.put(handler.getFeature(), target);
            wrappedOrdered.add(target);
        }

        FeatureHandlerRegistry finalRegistry = new FeatureHandlerRegistry(wrappedMap, wrappedOrdered);
        EngineRuntime.setFeatureHandlerRegistry(queueName, finalRegistry);

        List<com.openllmorchestrator.worker.engine.kernel.interceptor.ExecutionInterceptor> interceptors =
                new ArrayList<>(finalRegistry.getOrderedHandlers());
        ExecutionInterceptorChain chain = interceptors.isEmpty()
                ? ExecutionInterceptorChain.noOp()
                : new ExecutionInterceptorChain(interceptors);
        EngineRuntime.setExecutionInterceptorChain(queueName, chain);
    }
}
