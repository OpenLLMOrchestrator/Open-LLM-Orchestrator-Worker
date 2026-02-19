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
import com.openllmorchestrator.worker.engine.kernel.interceptor.ExecutionInterceptor;

/**
 * Feature-specific handler invoked at every node (capability) traversal in bootstrap-defined order.
 * Extends {@link ExecutionInterceptor}; the registry maintains a map (feature → handler) and an
 * ordered list of handlers so the execution hierarchy persists. Disabled features have no handler (null).
 */
public interface FeatureHandler extends ExecutionInterceptor {

    /** The feature this handler is for. Used for registry lookup and ordering. */
    FeatureFlag getFeature();
}
