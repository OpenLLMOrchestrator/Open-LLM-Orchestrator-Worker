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
package com.openllmorchestrator.worker.engine.kernel.merge;

import java.util.Map;

/**
 * Policy for merging one plugin's output into the accumulated output map.
 * Used after each SYNC plugin; also as the building block for ASYNC group merges.
 * Extensible: add new implementations for custom merge behaviour.
 */
public interface OutputMergePolicy {

    /**
     * Merge the current plugin's output into the accumulated map.
     *
     * @param accumulated mutable accumulated output (will be updated)
     * @param pluginOutput output from the current plugin (must not be null)
     * @param sourceId     plugin/activity name (for prefix or ordering; may be null)
     */
    void merge(Map<String, Object> accumulated, Map<String, Object> pluginOutput, String sourceId);
}
