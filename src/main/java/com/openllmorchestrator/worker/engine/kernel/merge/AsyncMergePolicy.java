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

import java.util.List;
import java.util.Map;

/**
 * Plugin contract for merging ASYNC group outputs into the accumulated map.
 * Implementations can be registered by name in {@link MergePolicyRegistry} and referenced in config.
 */
public interface AsyncMergePolicy {

    /**
     * Merge all ASYNC stage results into the accumulated output map.
     *
     * @param accumulated mutable accumulated output (updated in place)
     * @param results     list of (activityName, result) in completion or config order
     */
    void mergeAll(Map<String, Object> accumulated, List<AsyncOutputMergePolicy.NamedStageResult> results);
}
