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
 * One group in the plan: definitions + optional async completion policy and merge policy plugin name.
 * Graph-capable: optional dependencies (group indices that must complete before this group). Immutable.
 * Conditional: when {@link #getConditionDefinition()} is non-null, this is an if/elseif/else group;
 * the condition plugin runs first and must write output key {@code branch} (0=then, 1=first elseif, ..., n-1=else);
 * then the selected branch (list of CapabilityGroupSpec) runs as a sub-plan.
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class CapabilityGroupSpec {
    private final List<CapabilityDefinition> definitions;
    private final AsyncCompletionPolicy asyncPolicy;
    /** For ASYNC groups: merge policy activity name (plugin), invoked before exiting the group. Default LAST_WINS. */
    private final String asyncOutputMergePolicyName;
    /** DAG: group indices that must complete before this group. Empty = linear order (no deps). */
    private final int[] dependsOnGroupIndices;
    /** When non-null, this group is conditional: run this activity first, then run one of {@link #branches}. */
    private final CapabilityDefinition conditionDefinition;
    /** When conditional: branch 0 = then, 1..n-2 = elseif, n-1 = else. Each branch is a list of CapabilityGroupSpec to run in order. */
    private final List<List<CapabilityGroupSpec>> branches;

    public CapabilityGroupSpec(List<CapabilityDefinition> definitions, AsyncCompletionPolicy asyncPolicy) {
        this(definitions, asyncPolicy, "LAST_WINS", null, null, null);
    }

    public CapabilityGroupSpec(List<CapabilityDefinition> definitions, AsyncCompletionPolicy asyncPolicy,
                          String asyncOutputMergePolicyName) {
        this(definitions, asyncPolicy, asyncOutputMergePolicyName, null, null, null);
    }

    public CapabilityGroupSpec(List<CapabilityDefinition> definitions, AsyncCompletionPolicy asyncPolicy,
                          String asyncOutputMergePolicyName, int[] dependsOnGroupIndices) {
        this(definitions, asyncPolicy, asyncOutputMergePolicyName, dependsOnGroupIndices, null, null);
    }

    /** Conditional group: run conditionDefinition, then run branches.get(selectedIndex). */
    public CapabilityGroupSpec(CapabilityDefinition conditionDefinition, List<List<CapabilityGroupSpec>> branches) {
        this(Collections.emptyList(), AsyncCompletionPolicy.ALL, "LAST_WINS", null, conditionDefinition, branches);
    }

    @JsonCreator
    CapabilityGroupSpec(
            @JsonProperty("definitions") List<CapabilityDefinition> definitions,
            @JsonProperty("asyncPolicy") AsyncCompletionPolicy asyncPolicy,
            @JsonProperty("asyncOutputMergePolicyName") String asyncOutputMergePolicyName,
            @JsonProperty("dependsOnGroupIndices") int[] dependsOnGroupIndices,
            @JsonProperty("conditionDefinition") CapabilityDefinition conditionDefinition,
            @JsonProperty("branches") List<List<CapabilityGroupSpec>> branches) {
        this.definitions = definitions != null ? Collections.unmodifiableList(new ArrayList<>(definitions)) : Collections.emptyList();
        this.asyncPolicy = asyncPolicy != null ? asyncPolicy : AsyncCompletionPolicy.ALL;
        this.asyncOutputMergePolicyName = asyncOutputMergePolicyName != null && !asyncOutputMergePolicyName.isBlank()
                ? asyncOutputMergePolicyName : "LAST_WINS";
        this.dependsOnGroupIndices = dependsOnGroupIndices != null && dependsOnGroupIndices.length > 0
                ? dependsOnGroupIndices.clone()
                : new int[0];
        this.conditionDefinition = conditionDefinition;
        this.branches = branches != null ? Collections.unmodifiableList(new ArrayList<>(branches)) : null;
    }
}
