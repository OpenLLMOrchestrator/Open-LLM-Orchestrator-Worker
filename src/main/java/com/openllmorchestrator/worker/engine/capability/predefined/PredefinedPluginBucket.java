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
package com.openllmorchestrator.worker.engine.capability.predefined;

import com.openllmorchestrator.worker.contract.CapabilityHandler;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** Predefined stage name + plugin id → handler. */
public final class PredefinedPluginBucket {
    private static final String DEFAULT_PLUGIN_ID = "default";

    private final Map<String, Map<String, CapabilityHandler>> stageToPlugins;

    public PredefinedPluginBucket(Map<String, Map<String, CapabilityHandler>> stageToPlugins) {
        this.stageToPlugins = stageToPlugins == null ? Collections.emptyMap() : new HashMap<>(stageToPlugins);
    }

    public CapabilityHandler get(String stageName, String pluginId) {
        if (pluginId == null || pluginId.isBlank()) {
            pluginId = DEFAULT_PLUGIN_ID;
        }
        Map<String, CapabilityHandler> plugins = stageToPlugins.get(stageName);
        return plugins == null ? null : plugins.get(pluginId);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final Map<String, Map<String, CapabilityHandler>> stageToPlugins = new HashMap<>();

        public Builder register(String stageName, String pluginId, CapabilityHandler handler) {
            if (!PredefinedCapabilities.isPredefined(stageName)) {
                throw new IllegalArgumentException("Not a predefined stage: " + stageName);
            }
            if (handler == null) {
                throw new IllegalArgumentException("Handler must be non-null");
            }
            String pid = pluginId == null || pluginId.isBlank() ? DEFAULT_PLUGIN_ID : pluginId;
            stageToPlugins.computeIfAbsent(stageName, k -> new HashMap<>()).put(pid, handler);
            return this;
        }

        public PredefinedPluginBucket build() {
            return new PredefinedPluginBucket(stageToPlugins);
        }
    }
}

