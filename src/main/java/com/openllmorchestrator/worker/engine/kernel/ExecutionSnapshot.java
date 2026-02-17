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
package com.openllmorchestrator.worker.engine.kernel;

import com.openllmorchestrator.worker.engine.capability.CapabilityPlan;
import com.openllmorchestrator.worker.engine.contract.ContextSnapshot;
import lombok.Getter;

import java.util.Collections;
import java.util.Set;

/**
 * Immutable snapshot of kernel execution state at a point in time: which groups have completed
 * and a copy of context. Used for observability, debugging, and snapshot-aware replay/recovery.
 * Deterministic: produced from plan + context + completed set only.
 */
@Getter
public class ExecutionSnapshot {
    private final CapabilityPlan plan;
    /** Group indices that have completed (deterministic set). */
    private final Set<Integer> completedGroupIndices;
    private final ContextSnapshot contextSnapshot;

    public ExecutionSnapshot(CapabilityPlan plan, Set<Integer> completedGroupIndices, ContextSnapshot contextSnapshot) {
        this.plan = plan;
        this.completedGroupIndices = completedGroupIndices != null
                ? Collections.unmodifiableSet(completedGroupIndices)
                : Set.of();
        this.contextSnapshot = contextSnapshot;
    }
}

