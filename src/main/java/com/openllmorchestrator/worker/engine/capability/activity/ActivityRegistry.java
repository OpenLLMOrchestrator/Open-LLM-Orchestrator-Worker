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
package com.openllmorchestrator.worker.engine.capability.activity;

import com.openllmorchestrator.worker.contract.CapabilityHandler;
import com.openllmorchestrator.worker.engine.capability.handler.PluginActivityHandler;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Registry of activity/plugin name → CapabilityHandler. Used when pipeline uses stages with
 * group children as activity names; each child is one plugin implementing CapabilityHandler.
 */
public final class ActivityRegistry {

    /** All known plugin identifiers that can appear as group children. */
    public static final Set<String> KNOWN_PLUGIN_IDS = Set.of(
            "AccessControlPlugin", "TenantPolicyPlugin", "RateLimitPlugin",
            "MemoryPlugin", "VectorStorePlugin", "ModelPlugin", "MCPPlugin", "ToolPlugin",
            "FilterPlugin", "GuardrailPlugin", "RefinementPlugin", "PromptBuilderPlugin",
            "ObservabilityPlugin", "TracingPlugin", "BillingPlugin", "FeatureFlagPlugin", "AuditPlugin",
            "SecurityScannerPlugin", "CachingPlugin", "SearchPlugin", "LangChainAdapterPlugin",
            "AgentOrchestratorPlugin", "WorkflowExtensionPlugin", "CustomStagePlugin"
    );

    private final Map<String, CapabilityHandler> byName;

    public ActivityRegistry(Map<String, CapabilityHandler> byName) {
        this.byName = byName != null ? new HashMap<>(byName) : new HashMap<>();
    }

    public CapabilityHandler get(String activityName) {
        if (activityName == null || activityName.isBlank()) {
            return null;
        }
        return byName.get(activityName);
    }

    /** All registered handlers (for merging with dynamic plugins). */
    public Map<String, CapabilityHandler> getHandlers() {
        return Collections.unmodifiableMap(new HashMap<>(byName));
    }

    public boolean has(String activityName) {
        return get(activityName) != null;
    }

    public Set<String> registeredNames() {
        return Collections.unmodifiableSet(byName.keySet());
    }

    /** Default registry with all known plugin ids mapped to a stub handler. */
    public static ActivityRegistry createDefault() {
        Map<String, CapabilityHandler> map = new HashMap<>();
        for (String id : KNOWN_PLUGIN_IDS) {
            map.put(id, new PluginActivityHandler(id));
        }
        return new ActivityRegistry(map);
    }

    public static ActivityRegistry.Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final Map<String, CapabilityHandler> byName = new HashMap<>();

        public Builder register(String activityName, CapabilityHandler handler) {
            if (activityName != null && !activityName.isBlank() && handler != null) {
                byName.put(activityName, handler);
            }
            return this;
        }

        /** Add all handlers from an existing registry (e.g. before adding dynamic plugins). */
        public Builder registerAll(Map<String, CapabilityHandler> handlers) {
            if (handlers != null) {
                handlers.forEach(this::register);
            }
            return this;
        }

        public Builder registerAllDefaults() {
            for (String id : KNOWN_PLUGIN_IDS) {
                if (!byName.containsKey(id)) {
                    byName.put(id, new PluginActivityHandler(id));
                }
            }
            return this;
        }

        public ActivityRegistry build() {
            return new ActivityRegistry(byName);
        }
    }
}

