package com.openllmorchestrator.worker.engine.stage.activity;

import com.openllmorchestrator.worker.engine.stage.StageHandler;
import com.openllmorchestrator.worker.engine.stage.handler.PluginActivityHandler;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Registry of activity/plugin name → StageHandler. Used when pipeline uses stages with
 * group children as activity names; each child is one plugin implementing StageHandler.
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

    private final Map<String, StageHandler> byName;

    public ActivityRegistry(Map<String, StageHandler> byName) {
        this.byName = byName != null ? new HashMap<>(byName) : new HashMap<>();
    }

    public StageHandler get(String activityName) {
        if (activityName == null || activityName.isBlank()) {
            return null;
        }
        return byName.get(activityName);
    }

    /** All registered handlers (for merging with dynamic plugins). */
    public Map<String, StageHandler> getHandlers() {
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
        Map<String, StageHandler> map = new HashMap<>();
        for (String id : KNOWN_PLUGIN_IDS) {
            map.put(id, new PluginActivityHandler(id));
        }
        return new ActivityRegistry(map);
    }

    public static ActivityRegistry.Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final Map<String, StageHandler> byName = new HashMap<>();

        public Builder register(String activityName, StageHandler handler) {
            if (activityName != null && !activityName.isBlank() && handler != null) {
                byName.put(activityName, handler);
            }
            return this;
        }

        /** Add all handlers from an existing registry (e.g. before adding dynamic plugins). */
        public Builder registerAll(Map<String, StageHandler> handlers) {
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
