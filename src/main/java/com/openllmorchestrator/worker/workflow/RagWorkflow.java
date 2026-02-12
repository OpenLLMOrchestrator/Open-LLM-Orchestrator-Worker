package com.openllmorchestrator.worker.workflow;

import com.openllmorchestrator.worker.workflow.contract.RagRequest;
import com.openllmorchestrator.worker.workflow.contract.RagResponse;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface RagWorkflow {

    @WorkflowMethod
    RagResponse ask(RagRequest request);
}
