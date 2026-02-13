package com.openllmorchestrator.worker.engine.kernel;

import com.openllmorchestrator.worker.engine.activity.KernelStageActivity;
import com.openllmorchestrator.worker.engine.contract.ExecutionContext;
import com.openllmorchestrator.worker.engine.contract.StageResult;
import com.openllmorchestrator.worker.engine.stage.StageDefinition;
import com.openllmorchestrator.worker.engine.stage.StageRetryOptions;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Async;
import io.temporal.workflow.Promise;
import io.temporal.workflow.Workflow;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/** Passes pipeline data context to activities; no hardcoded timeouts/retries. */
public class StageInvoker {

    public Promise<StageResult> invokeAsync(StageDefinition definition, ExecutionContext context) {
        KernelStageActivity activity = Workflow.newActivityStub(KernelStageActivity.class, toActivityOptions(definition));
        Map<String, Object> orig = context != null ? context.getOriginalInput() : Map.of();
        Map<String, Object> acc = context != null ? context.getAccumulatedOutput() : Map.of();
        return Async.function(activity::execute, definition.getName(), orig, acc);
    }

    public StageResult invokeSync(StageDefinition definition, ExecutionContext context) {
        KernelStageActivity activity = Workflow.newActivityStub(KernelStageActivity.class, toActivityOptions(definition));
        Map<String, Object> orig = context != null ? context.getOriginalInput() : Map.of();
        Map<String, Object> acc = context != null ? context.getAccumulatedOutput() : Map.of();
        return activity.execute(definition.getName(), orig, acc);
    }

    private static ActivityOptions toActivityOptions(StageDefinition d) {
        ActivityOptions.Builder b = ActivityOptions.newBuilder()
                .setTaskQueue(d.getTaskQueue())
                .setStartToCloseTimeout(d.getTimeout());
        if (d.getScheduleToStartTimeout() != null) {
            b.setScheduleToStartTimeout(d.getScheduleToStartTimeout());
        }
        if (d.getScheduleToCloseTimeout() != null) {
            b.setScheduleToCloseTimeout(d.getScheduleToCloseTimeout());
        }
        if (d.getRetryOptions() != null) {
            b.setRetryOptions(toRetryOptions(d.getRetryOptions()));
        }
        return b.build();
    }

    private static RetryOptions toRetryOptions(StageRetryOptions r) {
        RetryOptions.Builder b = RetryOptions.newBuilder()
                .setMaximumAttempts(r.getMaximumAttempts())
                .setInitialInterval(Duration.ofSeconds(r.getInitialIntervalSeconds()))
                .setBackoffCoefficient(r.getBackoffCoefficient())
                .setMaximumInterval(Duration.ofSeconds(r.getMaximumIntervalSeconds()));
        List<String> nonRetryable = r.getNonRetryableErrors();
        if (nonRetryable != null && !nonRetryable.isEmpty()) {
            b.setDoNotRetry(nonRetryable.toArray(new String[0]));
        }
        return b.build();
    }
}
