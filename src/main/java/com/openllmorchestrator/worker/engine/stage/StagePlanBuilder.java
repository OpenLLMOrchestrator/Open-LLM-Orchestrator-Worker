package com.openllmorchestrator.worker.engine.stage;

import com.openllmorchestrator.worker.engine.kernel.merge.AsyncOutputMergePolicy;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Builds a StagePlan. All options from config; no hardcoded values. */
public final class StagePlanBuilder {
    private final List<StageGroupSpec> groups = new ArrayList<>();
    private int groupCounter = 0;

    StagePlanBuilder() {}

    public StagePlanBuilder addSyncWithCustomConfig(String stageName, StageExecutionMode mode,
                                                    Duration timeout, String taskQueue,
                                                    Duration scheduleToStart, Duration scheduleToClose,
                                                    StageRetryOptions retryOptions) {
        StageDefinition def = StageDefinition.builder()
                .name(stageName)
                .executionMode(mode)
                .group(groupCounter++)
                .taskQueue(taskQueue)
                .timeout(timeout)
                .scheduleToStartTimeout(scheduleToStart)
                .scheduleToCloseTimeout(scheduleToClose)
                .retryOptions(retryOptions)
                .build();
        groups.add(new StageGroupSpec(Collections.singletonList(def), null));
        return this;
    }

    public StagePlanBuilder addAsyncGroup(List<String> stageNames, Duration timeout, String taskQueue,
                                          Duration scheduleToStart, Duration scheduleToClose,
                                          StageRetryOptions retryOptions,
                                          AsyncCompletionPolicy asyncPolicy) {
        return addAsyncGroup(stageNames, timeout, taskQueue, scheduleToStart, scheduleToClose, retryOptions,
                asyncPolicy, null);
    }

    public StagePlanBuilder addAsyncGroup(List<String> stageNames, Duration timeout, String taskQueue,
                                          Duration scheduleToStart, Duration scheduleToClose,
                                          StageRetryOptions retryOptions,
                                          AsyncCompletionPolicy asyncPolicy,
                                          AsyncOutputMergePolicy asyncOutputMergePolicy) {
        List<StageDefinition> definitions = new ArrayList<>();
        for (String stageName : stageNames) {
            definitions.add(StageDefinition.builder()
                    .name(stageName)
                    .executionMode(StageExecutionMode.ASYNC)
                    .group(groupCounter)
                    .taskQueue(taskQueue)
                    .timeout(timeout)
                    .scheduleToStartTimeout(scheduleToStart)
                    .scheduleToCloseTimeout(scheduleToClose)
                    .retryOptions(retryOptions)
                    .build());
        }
        groupCounter++;
        groups.add(new StageGroupSpec(definitions, asyncPolicy != null ? asyncPolicy : AsyncCompletionPolicy.ALL,
                asyncOutputMergePolicy));
        return this;
    }

    public StagePlan build() {
        return new StagePlan(groups);
    }
}
