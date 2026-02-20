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
package com.openllmorchestrator.worker.engine.capability;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One node in the execution tree. Tree is built the same way as config:
 * capability → group → plugin (leaf), or recursive group/condition/expression.
 * Every node has a stable UUID (id) for pre/post handlers and debug state in Redis.
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExecutionTreeNode {
    /** Stable UUID for this node (assigned at build time). */
    private final String id;
    private final ExecutionNodeType type;
    /** Display name (capability name, plugin name, "condition", "branch-0", etc.). */
    private final String name;
    private final List<ExecutionTreeNode> children;

    @JsonCreator
    public ExecutionTreeNode(
            @JsonProperty("id") String id,
            @JsonProperty("type") ExecutionNodeType type,
            @JsonProperty("name") String name,
            @JsonProperty("children") List<ExecutionTreeNode> children) {
        this.id = id;
        this.type = type != null ? type : ExecutionNodeType.GROUP;
        this.name = name != null ? name : "";
        this.children = children != null ? Collections.unmodifiableList(new ArrayList<>(children)) : List.of();
    }

    public static ExecutionTreeNode of(String id, ExecutionNodeType type, String name) {
        return new ExecutionTreeNode(id, type, name, List.of());
    }

    public static ExecutionTreeNode withChildren(String id, ExecutionNodeType type, String name, List<ExecutionTreeNode> children) {
        return new ExecutionTreeNode(id, type, name, children != null ? children : List.of());
    }
}
