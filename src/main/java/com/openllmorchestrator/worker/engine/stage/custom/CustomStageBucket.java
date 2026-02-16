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
package com.openllmorchestrator.worker.engine.stage.custom;

import com.openllmorchestrator.worker.contract.StageHandler;
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

