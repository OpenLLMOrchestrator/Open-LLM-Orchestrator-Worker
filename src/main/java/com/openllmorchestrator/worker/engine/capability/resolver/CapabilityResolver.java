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
package com.openllmorchestrator.worker.engine.capability.resolver;

import com.openllmorchestrator.worker.engine.config.EngineFileConfig;
import com.openllmorchestrator.worker.engine.config.pipeline.CapabilityDef;
import com.openllmorchestrator.worker.contract.CapabilityHandler;
import com.openllmorchestrator.worker.engine.capability.activity.ActivityRegistry;
import com.openllmorchestrator.worker.engine.capability.custom.CustomCapabilityBucket;
import com.openllmorchestrator.worker.engine.capability.predefined.PredefinedPluginBucket;
import com.openllmorchestrator.worker.engine.capability.predefined.PredefinedCapabilities;

import java.util.Map;

/** Resolves capability/stage/activity name to handler: predefined capability → config-defined capability → activity (plugin) name → custom. */
public final class CapabilityResolver {
    private static final String DEFAULT_PLUGIN_ID = "default";

    private final EngineFileConfig config;
    private final PredefinedPluginBucket predefinedBucket;
    private final CustomCapabilityBucket customBucket;
    private final ActivityRegistry activityRegistry;

    public CapabilityResolver(EngineFileConfig config,
                         PredefinedPluginBucket predefinedBucket,
                         CustomCapabilityBucket customBucket) {
        this(config, predefinedBucket, customBucket, null);
    }

    public CapabilityResolver(EngineFileConfig config,
                         PredefinedPluginBucket predefinedBucket,
                         CustomCapabilityBucket customBucket,
                         ActivityRegistry activityRegistry) {
        this.config = config;
        this.predefinedBucket = predefinedBucket;
        this.customBucket = customBucket;
        this.activityRegistry = activityRegistry;
    }

    /**
     * Resolve a name to a handler. Order: predefined capability (by stagePlugins) → config-defined capability → activity/plugin name → custom bucket.
     */
    public CapabilityHandler resolve(String stageName) {
        if (stageName == null || stageName.isBlank()) {
            return null;
        }
        if (PredefinedCapabilities.isPredefined(stageName)) {
            String pluginId = getPluginIdForPredefined(stageName);
            CapabilityHandler h = predefinedBucket.get(stageName, pluginId);
            if (h != null) return h;
        }
        CapabilityDef customCap = config != null ? config.getCapabilitiesEffective().get(stageName) : null;
        if (customCap != null && customCap.getName() != null && !customCap.getName().isBlank() && activityRegistry != null) {
            CapabilityHandler h = activityRegistry.get(customCap.getName());
            if (h != null) return h;
        }
        if (activityRegistry != null) {
            CapabilityHandler h = activityRegistry.get(stageName);
            if (h != null) return h;
        }
        return customBucket.get(stageName);
    }

    public boolean canResolve(String stageName) {
        return resolve(stageName) != null;
    }

    private String getPluginIdForPredefined(String stageName) {
        if (config == null) {
            return DEFAULT_PLUGIN_ID;
        }
        Map<String, String> stagePlugins = config.getStagePluginsEffective();
        if (stagePlugins == null || stagePlugins.isEmpty()) {
            return DEFAULT_PLUGIN_ID;
        }
        String pluginId = stagePlugins.get(stageName);
        return pluginId == null || pluginId.isBlank() ? DEFAULT_PLUGIN_ID : pluginId;
    }
}

