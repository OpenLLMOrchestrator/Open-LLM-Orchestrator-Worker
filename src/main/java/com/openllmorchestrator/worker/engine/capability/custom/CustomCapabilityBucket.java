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
package com.openllmorchestrator.worker.engine.capability.custom;

import com.openllmorchestrator.worker.contract.CapabilityHandler;
import com.openllmorchestrator.worker.engine.capability.predefined.PredefinedCapabilities;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** Custom capability name → handler. Used for non-predefined capabilities. */
public final class CustomCapabilityBucket {
    private final Map<String, CapabilityHandler> handlers;

    public CustomCapabilityBucket(Map<String, CapabilityHandler> handlers) {
        this.handlers = handlers == null || handlers.isEmpty()
                ? Collections.emptyMap()
                : new HashMap<>(handlers);
    }

    public CapabilityHandler get(String capabilityName) {
        return handlers.get(capabilityName);
    }

    public boolean has(String capabilityName) {
        return handlers.containsKey(capabilityName);
    }

    public Set<String> capabilityNames() {
        return Collections.unmodifiableSet(handlers.keySet());
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final Map<String, CapabilityHandler> map = new HashMap<>();

        public Builder register(CapabilityHandler handler) {
            if (handler == null || handler.name() == null) {
                throw new IllegalArgumentException("CapabilityHandler and name must be non-null");
            }
            if (PredefinedCapabilities.isPredefined(handler.name())) {
                throw new IllegalArgumentException("Use PredefinedPluginBucket for predefined capability: " + handler.name());
            }
            if (map.containsKey(handler.name())) {
                throw new IllegalArgumentException("Duplicate custom capability name: " + handler.name());
            }
            map.put(handler.name(), handler);
            return this;
        }

        public CustomCapabilityBucket build() {
            return new CustomCapabilityBucket(map);
        }
    }
}

