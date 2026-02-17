/*
 * Copyright 2026 Open LLM Orchestrator contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use it except in compliance with the License.
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
package com.openllmorchestrator.worker.engine.kernel.execution;

import com.openllmorchestrator.worker.contract.StageResult;
import com.openllmorchestrator.worker.engine.contract.ExecutionContext;
import com.openllmorchestrator.worker.engine.contract.VersionedState;
import com.openllmorchestrator.worker.engine.kernel.StageInvoker;
import com.openllmorchestrator.worker.engine.kernel.interceptor.ExecutionInterceptorChain;
import com.openllmorchestrator.worker.engine.stage.StageDefinition;
import com.openllmorchestrator.worker.engine.stage.StageGroupSpec;
import com.openllmorchestrator.worker.engine.stage.StagePlan;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Executes conditional (if/elseif/else) groups: runs the condition plugin, reads output key {@code branch}
 * (0=then, 1=first elseif, ..., n-1=else), then runs the selected branch as a sub-plan.
 */
@Slf4j
public final class ConditionalGroupExecutor implements GroupExecutor {

    private static final String OUTPUT_KEY_BRANCH = "branch";

    private final BiConsumer<StagePlan, ExecutionContext> runSubPlan;

    public ConditionalGroupExecutor(BiConsumer<StagePlan, ExecutionContext> runSubPlan) {
        this.runSubPlan = runSubPlan != null ? runSubPlan : (p, c) -> {};
    }

    @Override
    public boolean supports(StageGroupSpec spec) {
        return spec != null && spec.getConditionDefinition() != null && spec.getBranches() != null && !spec.getBranches().isEmpty();
    }

    @Override
    public void execute(StageGroupSpec spec, StageInvoker invoker, ExecutionContext context,
                       int groupIndex, ExecutionInterceptorChain interceptorChain) {
        StageDefinition conditionDef = spec.getConditionDefinition();
        List<List<StageGroupSpec>> branches = spec.getBranches();
        log.info("Executing conditional group: condition plugin={}", conditionDef.getName());
        StageResult conditionResult = invoker.invokeSync(conditionDef, context);
        Map<String, Object> output = conditionResult != null ? conditionResult.getOutput() : null;
        if (output != null && !output.isEmpty()) {
            Map<String, Object> merged = new HashMap<>(context.getAccumulatedOutput());
            merged.putAll(output);
            VersionedState current = context.getVersionedState();
            if (current != null) {
                context.setVersionedState(current.withNextStep(merged));
            }
        }
        Object branchObj = output != null ? output.get(OUTPUT_KEY_BRANCH) : null;
        int branchIndex = toBranchIndex(branchObj, branches.size());
        log.info("Condition selected branch {} (0=then, {} = else)", branchIndex, branches.size() - 1);
        List<StageGroupSpec> selectedBranch = branches.get(branchIndex);
        StagePlan branchPlan = StagePlan.fromGroups(selectedBranch);
        runSubPlan.accept(branchPlan, context);
    }

    private static int toBranchIndex(Object value, int branchCount) {
        if (value == null) {
            return 0;
        }
        int idx;
        if (value instanceof Number) {
            idx = ((Number) value).intValue();
        } else if (value instanceof String) {
            try {
                idx = Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return 0;
            }
        } else {
            return 0;
        }
        if (idx < 0) return 0;
        if (idx >= branchCount) return branchCount - 1;
        return idx;
    }
}
