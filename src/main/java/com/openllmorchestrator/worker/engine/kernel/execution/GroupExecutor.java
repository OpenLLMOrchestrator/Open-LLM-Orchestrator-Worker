package com.openllmorchestrator.worker.engine.kernel.execution;

import com.openllmorchestrator.worker.engine.contract.ExecutionContext;
import com.openllmorchestrator.worker.engine.kernel.StageInvoker;
import com.openllmorchestrator.worker.engine.stage.StageGroupSpec;

/** Executes one group of stages. Add new modes by adding implementations (OCP). */
public interface GroupExecutor {
    boolean supports(StageGroupSpec spec);

    void execute(StageGroupSpec spec, StageInvoker invoker, ExecutionContext context);
}
