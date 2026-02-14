package com.openllmorchestrator.worker.workflow;

import com.openllmorchestrator.worker.engine.contract.ExecutionCommand;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

import java.util.Map;

@WorkflowInterface
public interface CoreWorkflow {

    /** Execute the pipeline and return the accumulated output (includes "result" from LLM) as the response. */
    @WorkflowMethod
    Map<String, Object> execute(ExecutionCommand command);
}
