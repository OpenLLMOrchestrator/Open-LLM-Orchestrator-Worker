package com.openllmorchestrator.worker.workflow.impl;

import com.openllmorchestrator.worker.engine.contract.ExecutionCommand;
import com.openllmorchestrator.worker.engine.contract.ExecutionContext;
import com.openllmorchestrator.worker.engine.kernel.KernelOrchestrator;
import com.openllmorchestrator.worker.engine.kernel.StageInvoker;
import com.openllmorchestrator.worker.engine.runtime.EngineRuntime;
import com.openllmorchestrator.worker.engine.stage.StagePlan;
import com.openllmorchestrator.worker.workflow.CoreWorkflow;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

/**
 * Uses one-time bootstrapped plan and invoker; no config read per execution.
 */
public class CoreWorkflowImpl implements CoreWorkflow {

    private static final Logger log = Workflow.getLogger(CoreWorkflowImpl.class);

    @Override
    public void execute(ExecutionCommand command) {
        ExecutionContext context = ExecutionContext.from(command);
        StagePlan plan = EngineRuntime.getStagePlan();
        StageInvoker invoker = new StageInvoker();
        KernelOrchestrator kernel = new KernelOrchestrator(invoker);
        kernel.execute(plan, context);
    }
}
