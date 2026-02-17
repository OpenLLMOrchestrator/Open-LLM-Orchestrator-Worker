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

import com.openllmorchestrator.worker.engine.capability.CapabilityGroupSpec;
import com.openllmorchestrator.worker.engine.capability.CapabilityPlan;
import com.openllmorchestrator.worker.engine.contract.ExecutionContext;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Execution-state driven kernel state: plan, context, and completed group indices.
 * "What to run next" is derived deterministically from this state (ready set from plan graph).
 * Snapshot-aware: can produce an {@link ExecutionSnapshot} at any time.
 */
@Getter
public class ExecutionState {
    private final CapabilityPlan plan;
    private final ExecutionContext context;
    private final Set<Integer> completedGroupIndices;

    public ExecutionState(CapabilityPlan plan, ExecutionContext context) {
        this.plan = plan;
        this.context = context;
        this.completedGroupIndices = new HashSet<>();
    }

    /** Deterministic: returns group indices that are ready (all dependencies completed), in ascending order. */
    public List<Integer> getReadyGroupIndices() {
        List<CapabilityGroupSpec> groups = plan.getGroups();
        if (groups.isEmpty()) return List.of();
        List<Integer> ready = new ArrayList<>();
        for (int i = 0; i < groups.size(); i++) {
            if (completedGroupIndices.contains(i)) continue;
            int[] deps = groups.get(i).getDependsOnGroupIndices();
            boolean allDone = true;
            if (deps != null) {
                for (int d : deps) {
                    if (d >= 0 && d < groups.size() && !completedGroupIndices.contains(d)) {
                        allDone = false;
                        break;
                    }
                }
            }
            if (allDone) ready.add(i);
        }
        Collections.sort(ready);
        return ready;
    }

    public void markCompleted(int groupIndex) {
        completedGroupIndices.add(groupIndex);
    }

    public boolean isDone() {
        return completedGroupIndices.size() >= plan.getGroups().size();
    }

    /** Snapshot of current state for observability/recovery. Deterministic given current state. */
    public ExecutionSnapshot snapshot() {
        return new ExecutionSnapshot(
                plan,
                new HashSet<>(completedGroupIndices),
                context.snapshot()
        );
    }
}

