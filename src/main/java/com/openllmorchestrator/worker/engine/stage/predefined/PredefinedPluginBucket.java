package com.openllmorchestrator.worker.engine.stage.predefined;

import com.openllmorchestrator.worker.engine.stage.StageHandler;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** Predefined stage name + plugin id → handler. */
public final class PredefinedPluginBucket {
    private static final String DEFAULT_PLUGIN_ID = "default";

    private final Map<String, Map<String, StageHandler>> stageToPlugins;

    public PredefinedPluginBucket(Map<String, Map<String, StageHandler>> stageToPlugins) {
        this.stageToPlugins = stageToPlugins == null ? Collections.emptyMap() : new HashMap<>(stageToPlugins);
    }

    public StageHandler get(String stageName, String pluginId) {
        if (pluginId == null || pluginId.isBlank()) {
            pluginId = DEFAULT_PLUGIN_ID;
        }
        Map<String, StageHandler> plugins = stageToPlugins.get(stageName);
        return plugins == null ? null : plugins.get(pluginId);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final Map<String, Map<String, StageHandler>> stageToPlugins = new HashMap<>();

        public Builder register(String stageName, String pluginId, StageHandler handler) {
            if (!PredefinedStages.isPredefined(stageName)) {
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
