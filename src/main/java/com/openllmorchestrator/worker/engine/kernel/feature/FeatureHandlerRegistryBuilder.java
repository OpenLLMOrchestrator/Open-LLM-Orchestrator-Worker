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
package com.openllmorchestrator.worker.engine.kernel.feature;

import com.openllmorchestrator.worker.engine.config.FeatureFlag;
import com.openllmorchestrator.worker.engine.config.FeatureFlags;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds {@link FeatureHandlerRegistry} at bootstrap from enabled features and a factory.
 * Only enabled features get a handler; order is taken from {@link FeatureHandlerOrder}.
 */
public final class FeatureHandlerRegistryBuilder {

    private FeatureHandlerRegistryBuilder() {}

    /**
     * Build registry: for each feature in {@link FeatureHandlerOrder#getDefaultOrder()} that is enabled,
     * create a handler via the factory. Map and list contain only non-null handlers (disabled = not in map, not in list).
     */
    public static FeatureHandlerRegistry build(FeatureFlags featureFlags, FeatureHandlerFactory factory) {
        if (featureFlags == null || factory == null) {
            return FeatureHandlerRegistry.empty();
        }
        Map<FeatureFlag, FeatureHandler> map = new LinkedHashMap<>();
        List<FeatureHandler> ordered = new ArrayList<>();
        for (FeatureFlag feature : FeatureHandlerOrder.getDefaultOrder()) {
            if (!featureFlags.isEnabled(feature)) {
                continue;
            }
            FeatureHandler handler = factory.createHandler(feature);
            if (handler != null) {
                map.put(feature, handler);
                ordered.add(handler);
            }
        }
        return new FeatureHandlerRegistry(map, ordered);
    }
}
