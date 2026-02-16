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
package com.openllmorchestrator.worker.engine.stage;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One group in the plan: definitions + optional async completion policy and merge policy plugin name.
 * Graph-capable: optional dependencies (group indices that must complete before this group). Immutable.
 */
@Getter
public class StageGroupSpec {
    private final List<StageDefinition> definitions;
    private final AsyncCompletionPolicy asyncPolicy;
    /** For ASYNC groups: merge policy activity name (plugin), invoked before exiting the group. Default LAST_WINS. */
    private final String asyncOutputMergePolicyName;
    /** DAG: group indices that must complete before this group. Empty = linear order (no deps). */
    private final int[] dependsOnGroupIndices;

    public StageGroupSpec(List<StageDefinition> definitions, AsyncCompletionPolicy asyncPolicy) {
        this(definitions, asyncPolicy, "LAST_WINS", null);
    }

    public StageGroupSpec(List<StageDefinition> definitions, AsyncCompletionPolicy asyncPolicy,
                          String asyncOutputMergePolicyName) {
        this(definitions, asyncPolicy, asyncOutputMergePolicyName, null);
    }

    public StageGroupSpec(List<StageDefinition> definitions, AsyncCompletionPolicy asyncPolicy,
                          String asyncOutputMergePolicyName, int[] dependsOnGroupIndices) {
        this.definitions = Collections.unmodifiableList(new ArrayList<>(definitions));
        this.asyncPolicy = asyncPolicy != null ? asyncPolicy : AsyncCompletionPolicy.ALL;
        this.asyncOutputMergePolicyName = asyncOutputMergePolicyName != null && !asyncOutputMergePolicyName.isBlank()
                ? asyncOutputMergePolicyName : "LAST_WINS";
        this.dependsOnGroupIndices = dependsOnGroupIndices != null && dependsOnGroupIndices.length > 0
                ? dependsOnGroupIndices.clone()
                : new int[0];
    }
}

