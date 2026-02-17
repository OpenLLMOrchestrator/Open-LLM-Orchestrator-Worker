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

/** Predefined capability name + plugin id → handler. */
public final class PredefinedPluginBucket {
    private static final String DEFAULT_PLUGIN_ID = "default";

    private final Map<String, Map<String, CapabilityHandler>> capabilityToPlugins;

    public PredefinedPluginBucket(Map<String, Map<String, CapabilityHandler>> capabilityToPlugins) {
        this.capabilityToPlugins = capabilityToPlugins == null ? Collections.emptyMap() : new HashMap<>(capabilityToPlugins);
    }

    public CapabilityHandler get(String capabilityName, String pluginId) {
        if (pluginId == null || pluginId.isBlank()) {
            pluginId = DEFAULT_PLUGIN_ID;
        }
        Map<String, CapabilityHandler> plugins = capabilityToPlugins.get(capabilityName);
        return plugins == null ? null : plugins.get(pluginId);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final Map<String, Map<String, CapabilityHandler>> capabilityToPlugins = new HashMap<>();

        public Builder register(String capabilityName, String pluginId, CapabilityHandler handler) {
            if (!PredefinedCapabilities.isPredefined(capabilityName)) {
                throw new IllegalArgumentException("Not a predefined capability: " + capabilityName);
            }
            if (handler == null) {
                throw new IllegalArgumentException("Handler must be non-null");
            }
            String pid = pluginId == null || pluginId.isBlank() ? DEFAULT_PLUGIN_ID : pluginId;
            capabilityToPlugins.computeIfAbsent(capabilityName, k -> new HashMap<>()).put(pid, handler);
            return this;
        }

        public PredefinedPluginBucket build() {
            return new PredefinedPluginBucket(capabilityToPlugins);
        }
    }
}

