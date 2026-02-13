package com.openllmorchestrator.worker.workflow;

import com.openllmorchestrator.worker.engine.contract.ExecutionCommand;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface CoreWorkflow {

    @WorkflowMethod
    void execute(ExecutionCommand command);
}
