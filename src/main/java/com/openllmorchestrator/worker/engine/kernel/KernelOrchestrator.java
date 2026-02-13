package com.openllmorchestrator.worker.engine.kernel;

import com.openllmorchestrator.worker.engine.contract.ExecutionContext;
import com.openllmorchestrator.worker.engine.kernel.execution.AsyncGroupExecutor;
import com.openllmorchestrator.worker.engine.kernel.execution.GroupExecutor;
import com.openllmorchestrator.worker.engine.kernel.execution.SyncGroupExecutor;
import com.openllmorchestrator.worker.engine.stage.StageGroupSpec;
import com.openllmorchestrator.worker.engine.stage.StagePlan;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class KernelOrchestrator {
    private static final List<GroupExecutor> EXECUTORS = List.of(
            new SyncGroupExecutor(),
            new AsyncGroupExecutor()
    );

    private final StageInvoker stageInvoker;

    public KernelOrchestrator(StageInvoker stageInvoker) {
        this.stageInvoker = stageInvoker;
    }

    public void execute(StagePlan plan, ExecutionContext context) {
        for (StageGroupSpec spec : plan.getGroups()) {
            log.info("---- Executing Stage Group ----");
            executeGroup(spec, context);
        }
    }

    private void executeGroup(StageGroupSpec spec, ExecutionContext context) {
        for (GroupExecutor ex : EXECUTORS) {
            if (ex.supports(spec)) {
                ex.execute(spec, stageInvoker, context);
                return;
            }
        }
        throw new IllegalStateException("No executor for group");
    }
}
