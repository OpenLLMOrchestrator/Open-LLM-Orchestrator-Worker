package com.openllmorchestrator.worker.engine.kernel;

import com.openllmorchestrator.worker.engine.activity.KernelStageActivity;
import com.openllmorchestrator.worker.engine.contract.StageResult;
import com.openllmorchestrator.worker.engine.stage.StageDefinition;
import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Async;
import io.temporal.workflow.Promise;
import io.temporal.workflow.Workflow;

public class StageInvoker {

    public Promise<StageResult> invokeAsync(StageDefinition definition) {

        ActivityOptions options =
                ActivityOptions.newBuilder()
                        .setTaskQueue(definition.getTaskQueue())
                        .setStartToCloseTimeout(definition.getTimeout())
                        .build();

        KernelStageActivity activity =
                Workflow.newActivityStub(KernelStageActivity.class, options);

        return Async.function(activity::execute,
                definition.getName());
    }

    public StageResult invokeSync(StageDefinition definition) {

        ActivityOptions options =
                ActivityOptions.newBuilder()
                        .setTaskQueue(definition.getTaskQueue())
                        .setStartToCloseTimeout(definition.getTimeout())
                        .build();

        KernelStageActivity activity =
                Workflow.newActivityStub(KernelStageActivity.class, options);

        return activity.execute(definition.getName());
    }
}
