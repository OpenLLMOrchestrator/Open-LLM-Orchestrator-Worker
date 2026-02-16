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
package com.openllmorchestrator.worker.engine.config.pipeline;

import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.List;

/**
 * A group within a stage: sync or async execution, with recursive nesting.
 * Children are activity names (plugin ids) or nested GroupConfigs.
 */
@Getter
@Setter
public class GroupConfig {
    /** SYNC or ASYNC */
    private String executionMode;
    /** For ASYNC: ALL | FIRST_SUCCESS | FIRST_FAILURE | ALL_SETTLED */
    private String asyncCompletionPolicy;
    /** For ASYNC: output key overwrite policy (name from merge policy registry). Legacy; prefer mergePolicy hook. */
    private String asyncOutputMergePolicy;
    /** For ASYNC: merge policy hook { "type": "MERGE_POLICY", "pluginType": "MergePolicy", "name": "..." }. */
    private MergePolicyConfig mergePolicy;
    /** Max recursion depth for nested groups (overrides pipeline defaultMaxGroupDepth). */
    private Integer maxDepth;
    private Integer timeoutSeconds;
    /**
     * Children: each element is either a String (activity/plugin name) or a Map (nested group).
     * Use {@link #getChildrenAsList()} and interpret per element.
     */
    private List<Object> children;

    public List<Object> getChildrenAsList() {
        return children != null ? children : Collections.emptyList();
    }

    public boolean isAsync() {
        return "ASYNC".equalsIgnoreCase(executionMode);
    }

    public boolean isSync() {
        return "SYNC".equalsIgnoreCase(executionMode);
    }
}

