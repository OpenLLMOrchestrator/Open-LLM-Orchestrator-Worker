package com.openllmorchestrator.worker.engine.stage.custom;

import com.openllmorchestrator.worker.engine.stage.StageHandler;
import com.openllmorchestrator.worker.engine.stage.predefined.PredefinedStages;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** Custom stage name → handler. Used for non-predefined stages. */
public final class CustomStageBucket {
    private final Map<String, StageHandler> handlers;

    public CustomStageBucket(Map<String, StageHandler> handlers) {
        this.handlers = handlers == null || handlers.isEmpty()
                ? Collections.emptyMap()
                : new HashMap<>(handlers);
    }

    public StageHandler get(String stageName) {
        return handlers.get(stageName);
    }

    public boolean has(String stageName) {
        return handlers.containsKey(stageName);
    }

    public Set<String> stageNames() {
        return Collections.unmodifiableSet(handlers.keySet());
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final Map<String, StageHandler> map = new HashMap<>();

        public Builder register(StageHandler handler) {
            if (handler == null || handler.name() == null) {
                throw new IllegalArgumentException("StageHandler and name must be non-null");
            }
            if (PredefinedStages.isPredefined(handler.name())) {
                throw new IllegalArgumentException("Use PredefinedPluginBucket for predefined stage: " + handler.name());
            }
            if (map.containsKey(handler.name())) {
                throw new IllegalArgumentException("Duplicate custom stage name: " + handler.name());
            }
            map.put(handler.name(), handler);
            return this;
        }

        public CustomStageBucket build() {
            return new CustomStageBucket(map);
        }
    }
}
