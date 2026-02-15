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

import com.openllmorchestrator.worker.engine.config.activity.RetryPolicyConfig;
import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.List;

/** A node in the pipeline tree: GROUP or STAGE. */
@Getter
@Setter
public class NodeConfig {
    private String type;
    /** For STAGE: class name to call (fully qualified class name, e.g. com.example.plugin.AccessControlPluginImpl). Required. */
    private String name;
    /** For STAGE: one of the allowed plugin types (e.g. AccessControlPlugin, MemoryPlugin). Required. */
    private String pluginType;
    private String executionMode;
    private Integer timeoutSeconds;
    /** For GROUP ASYNC: override default policy (ALL | FIRST_SUCCESS | FIRST_FAILURE | ALL_SETTLED). */
    private String asyncCompletionPolicy;
    /** For GROUP ASYNC: output key overwrite policy (name from merge policy registry). Legacy; prefer mergePolicy hook. */
    private String asyncOutputMergePolicy;
    /** For GROUP ASYNC: merge policy hook { "type": "MERGE_POLICY", "pluginType": "MergePolicy", "name": "..." }. */
    private MergePolicyConfig mergePolicy;
    /** For GROUP: max recursion depth for nested groups (overrides pipeline defaultMaxGroupDepth). */
    private Integer maxDepth;
    /** For STAGE: optional activity timeout overrides (seconds). */
    private Integer scheduleToStartSeconds;
    private Integer scheduleToCloseSeconds;
    /** For STAGE: optional retry override. */
    private RetryPolicyConfig retryPolicy;
    private List<NodeConfig> children;

    public List<NodeConfig> getChildren() {
        return children != null ? children : Collections.emptyList();
    }

    public boolean isGroup() {
        return "GROUP".equalsIgnoreCase(type);
    }

    public boolean isStage() {
        return "STAGE".equalsIgnoreCase(type);
    }
}
