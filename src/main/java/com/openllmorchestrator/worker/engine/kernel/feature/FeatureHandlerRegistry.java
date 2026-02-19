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

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Immutable registry built at bootstrap: map (feature → handler) and ordered list of handlers.
 * Only enabled features have a non-null handler. At runtime the kernel invokes
 * {@link #getOrderedHandlers()} at every node traversal so each registered feature runs in list order.
 */
public final class FeatureHandlerRegistry {

    private final Map<FeatureFlag, FeatureHandler> handlerByFeature;
    private final List<FeatureHandler> orderedHandlers;

    public FeatureHandlerRegistry(Map<FeatureFlag, FeatureHandler> handlerByFeature,
                                  List<FeatureHandler> orderedHandlers) {
        this.handlerByFeature = handlerByFeature != null
                ? Collections.unmodifiableMap(handlerByFeature)
                : Collections.emptyMap();
        this.orderedHandlers = orderedHandlers != null
                ? Collections.unmodifiableList(orderedHandlers)
                : Collections.emptyList();
    }

    /** Handler for the feature, or null if feature is disabled or has no handler. */
    public FeatureHandler getHandler(FeatureFlag feature) {
        return feature != null ? handlerByFeature.get(feature) : null;
    }

    /** Ordered list of handlers to run at every node traversal (before/after capability). No nulls. */
    public List<FeatureHandler> getOrderedHandlers() {
        return orderedHandlers;
    }

    /** Empty registry (no feature handlers; fast core path). */
    public static FeatureHandlerRegistry empty() {
        return new FeatureHandlerRegistry(Collections.emptyMap(), Collections.emptyList());
    }
}
