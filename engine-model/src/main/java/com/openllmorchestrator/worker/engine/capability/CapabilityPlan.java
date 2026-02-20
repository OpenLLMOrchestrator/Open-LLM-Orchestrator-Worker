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
 * Execution hierarchy: ordered list of stage groups. Built once at bootstrap from config
 * and reused for the container lifecycle. Immutable; serializable.
 * Graph-capable: each group may declare {@link CapabilityGroupSpec#getDependsOnGroupIndices()}; kernel runs
 * a group when all its dependencies have completed (deterministic ready set).
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class CapabilityPlan {
    private final List<CapabilityGroupSpec> groups;

    @JsonCreator
    CapabilityPlan(@JsonProperty("groups") List<CapabilityGroupSpec> groups) {
        this.groups = groups != null ? Collections.unmodifiableList(new ArrayList<>(groups)) : List.of();
    }

    public static CapabilityPlanBuilder builder() {
        return new CapabilityPlanBuilder();
    }

    /** Build a plan from a list of group specs (e.g. for conditional branch sub-plans). */
    public static CapabilityPlan fromGroups(List<CapabilityGroupSpec> groups) {
        return new CapabilityPlan(groups != null ? groups : List.of());
    }

    /**
     * Returns a copy of this plan for execution-scoped use. The copy is stored in execution context
     * before any stage can modify the hierarchy (e.g. planner). Modifications apply only to the copy; the global
     * per-queue execution tree remains immutable.
     */
    public CapabilityPlan copyForExecution() {
        return fromGroups(new ArrayList<>(getGroups()));
    }
}
