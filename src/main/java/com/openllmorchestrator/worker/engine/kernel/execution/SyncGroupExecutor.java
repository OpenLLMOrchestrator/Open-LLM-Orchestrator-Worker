package com.openllmorchestrator.worker.engine.kernel.execution;

import com.openllmorchestrator.worker.engine.contract.ExecutionContext;
import com.openllmorchestrator.worker.engine.contract.StageResult;
import com.openllmorchestrator.worker.engine.kernel.StageInvoker;
import com.openllmorchestrator.worker.engine.kernel.merge.OutputMergePolicy;
import com.openllmorchestrator.worker.engine.kernel.merge.PutAllMergePolicy;
import com.openllmorchestrator.worker.engine.stage.StageDefinition;
import com.openllmorchestrator.worker.engine.stage.StageExecutionMode;
import com.openllmorchestrator.worker.engine.stage.StageGroupSpec;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public final class SyncGroupExecutor implements GroupExecutor {
    private static final OutputMergePolicy SYNC_MERGE = PutAllMergePolicy.INSTANCE;

    @Override
    public boolean supports(StageGroupSpec spec) {
        return spec != null && spec.getDefinitions() != null && !spec.getDefinitions().isEmpty()
                && spec.getDefinitions().get(0).getExecutionMode() == StageExecutionMode.SYNC;
    }

    @Override
    public void execute(StageGroupSpec spec, StageInvoker invoker, ExecutionContext context) {
        for (StageDefinition def : spec.getDefinitions()) {
            log.info("Executing SYNC stage: {}", def.getName());
            StageResult result = invoker.invokeSync(def, context);
            Map<String, Object> output = result.getData() != null ? result.getData() : Map.of();
            SYNC_MERGE.merge(context.getAccumulatedOutput(), output, def.getName());
            log.info("Completed SYNC stage: {}", result.getStageName());
        }
    }
}
