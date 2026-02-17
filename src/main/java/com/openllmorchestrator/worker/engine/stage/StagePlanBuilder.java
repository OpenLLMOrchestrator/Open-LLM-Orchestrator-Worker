/*
 * Copyright 2026 Open LLM Orchestrator contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.openllmorchestrator.worker.engine.stage;

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
        return addSyncWithCustomConfig(stageName, mode, timeout, taskQueue, scheduleToStart, scheduleToClose, retryOptions, null);
    }

    public StagePlanBuilder addSyncWithCustomConfig(String stageName, StageExecutionMode mode,
                                                    Duration timeout, String taskQueue,
                                                    Duration scheduleToStart, Duration scheduleToClose,
                                                    StageRetryOptions retryOptions,
                                                    String stageBucketName) {
        StageDefinition def = StageDefinition.builder()
                .name(stageName)
                .executionMode(mode)
                .group(groupCounter++)
                .taskQueue(taskQueue)
                .timeout(timeout)
                .scheduleToStartTimeout(scheduleToStart)
                .scheduleToCloseTimeout(scheduleToClose)
                .retryOptions(retryOptions)
                .stageBucketName(stageBucketName)
                .build();
        groups.add(new StageGroupSpec(Collections.singletonList(def), null));
        return this;
    }

    public StagePlanBuilder addAsyncGroup(List<String> stageNames, Duration timeout, String taskQueue,
                                          Duration scheduleToStart, Duration scheduleToClose,
                                          StageRetryOptions retryOptions,
                                          AsyncCompletionPolicy asyncPolicy) {
        return addAsyncGroup(stageNames, timeout, taskQueue, scheduleToStart, scheduleToClose, retryOptions,
                asyncPolicy, "LAST_WINS");
    }

    public StagePlanBuilder addAsyncGroup(List<String> stageNames, Duration timeout, String taskQueue,
                                          Duration scheduleToStart, Duration scheduleToClose,
                                          StageRetryOptions retryOptions,
                                          AsyncCompletionPolicy asyncPolicy,
                                          String asyncOutputMergePolicyName) {
        return addAsyncGroup(stageNames, timeout, taskQueue, scheduleToStart, scheduleToClose, retryOptions, asyncPolicy, asyncOutputMergePolicyName, null);
    }

    public StagePlanBuilder addAsyncGroup(List<String> stageNames, Duration timeout, String taskQueue,
                                          Duration scheduleToStart, Duration scheduleToClose,
                                          StageRetryOptions retryOptions,
                                          AsyncCompletionPolicy asyncPolicy,
                                          String asyncOutputMergePolicyName,
                                          String stageBucketName) {
        List<StageDefinition> definitions = new ArrayList<>();
        for (String name : stageNames) {
            definitions.add(StageDefinition.builder()
                    .name(name)
                    .executionMode(StageExecutionMode.ASYNC)
                    .group(groupCounter)
                    .taskQueue(taskQueue)
                    .timeout(timeout)
                    .scheduleToStartTimeout(scheduleToStart)
                    .scheduleToCloseTimeout(scheduleToClose)
                    .retryOptions(retryOptions)
                    .stageBucketName(stageBucketName)
                    .build());
        }
        groupCounter++;
        groups.add(new StageGroupSpec(definitions, asyncPolicy != null ? asyncPolicy : AsyncCompletionPolicy.ALL,
                asyncOutputMergePolicyName));
        return this;
    }

    /**
     * Add a conditional group (if/elseif/else): run condition activity first, then run one of the branches.
     * Condition plugin must write output key {@code branch} (Integer: 0=then, 1=first elseif, ..., n-1=else).
     */
    public StagePlanBuilder addConditionalGroup(StageDefinition conditionDefinition, List<List<StageGroupSpec>> branches) {
        if (conditionDefinition == null || branches == null || branches.isEmpty()) {
            throw new IllegalArgumentException("conditionDefinition and non-empty branches required");
        }
        groups.add(new StageGroupSpec(conditionDefinition, branches));
        return this;
    }

    public StagePlan build() {
        return new StagePlan(groups);
    }
}

