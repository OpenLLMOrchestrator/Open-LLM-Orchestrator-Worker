package com.openllmorchestrator.worker.engine.kernel.execution;

import com.openllmorchestrator.worker.engine.contract.ExecutionContext;
import com.openllmorchestrator.worker.engine.contract.StageResult;
import com.openllmorchestrator.worker.engine.kernel.StageInvoker;
import com.openllmorchestrator.worker.engine.kernel.merge.AsyncOutputMergePolicy;
import com.openllmorchestrator.worker.engine.stage.AsyncCompletionPolicy;
import com.openllmorchestrator.worker.engine.stage.StageDefinition;
import com.openllmorchestrator.worker.engine.stage.StageExecutionMode;
import com.openllmorchestrator.worker.engine.stage.StageGroupSpec;
import io.temporal.workflow.Promise;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public final class AsyncGroupExecutor implements GroupExecutor {
    @Override
    public boolean supports(StageGroupSpec spec) {
        return spec != null && spec.getDefinitions() != null && !spec.getDefinitions().isEmpty()
                && spec.getDefinitions().get(0).getExecutionMode() == StageExecutionMode.ASYNC;
    }

    @Override
    public void execute(StageGroupSpec spec, StageInvoker invoker, ExecutionContext context) {
        List<StageDefinition> group = spec.getDefinitions();
        AsyncCompletionPolicy policy = spec.getAsyncPolicy() != null ? spec.getAsyncPolicy() : AsyncCompletionPolicy.ALL;
        List<Promise<StageResult>> promises = new ArrayList<>();
        for (StageDefinition def : group) {
            log.info("Scheduling ASYNC stage: {}", def.getName());
            promises.add(invoker.invokeAsync(def, context));
        }
        waitForPromises(policy, promises);
        List<String> names = new ArrayList<>(group.size());
        List<StageResult> results = new ArrayList<>(group.size());
        for (int i = 0; i < group.size(); i++) {
            StageDefinition def = group.get(i);
            names.add(def.getName());
            try {
                results.add(promises.get(i).get());
            } catch (Exception e) {
                results.add(StageResult.builder().stageName(def.getName()).build());
            }
        }
        spec.getAsyncOutputMergePolicy().mergeAll(context.getAccumulatedOutput(),
                AsyncOutputMergePolicy.NamedStageResult.from(names, results));
        for (StageResult r : results) {
            log.info("Completed ASYNC stage: {}", r.getStageName());
        }
    }

    private static void waitForPromises(AsyncCompletionPolicy policy, List<Promise<StageResult>> promises) {
        switch (policy) {
            case ALL:
            case FIRST_FAILURE:
            case ALL_SETTLED:
                Promise.allOf(promises).get();
                break;
            case FIRST_SUCCESS:
                Promise.anyOf(promises).get();
                break;
            default:
                Promise.allOf(promises).get();
        }
    }
}
