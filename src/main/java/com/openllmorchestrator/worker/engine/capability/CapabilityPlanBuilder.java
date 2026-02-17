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
package com.openllmorchestrator.worker.engine.capability;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Builds a CapabilityPlan. All options from config; no hardcoded values. */
public final class CapabilityPlanBuilder {
    private final List<CapabilityGroupSpec> groups = new ArrayList<>();
    private int groupCounter = 0;

    CapabilityPlanBuilder() {}

    public CapabilityPlanBuilder addSyncWithCustomConfig(String stageName, CapabilityExecutionMode mode,
                                                    Duration timeout, String taskQueue,
                                                    Duration scheduleToStart, Duration scheduleToClose,
                                                    CapabilityRetryOptions retryOptions) {
        return addSyncWithCustomConfig(stageName, mode, timeout, taskQueue, scheduleToStart, scheduleToClose, retryOptions, null);
    }

    public CapabilityPlanBuilder addSyncWithCustomConfig(String stageName, CapabilityExecutionMode mode,
                                                    Duration timeout, String taskQueue,
                                                    Duration scheduleToStart, Duration scheduleToClose,
                                                    CapabilityRetryOptions retryOptions,
                                                    String stageBucketName) {
        CapabilityDefinition def = CapabilityDefinition.builder()
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
        groups.add(new CapabilityGroupSpec(Collections.singletonList(def), null));
        return this;
    }

    public CapabilityPlanBuilder addAsyncGroup(List<String> stageNames, Duration timeout, String taskQueue,
                                          Duration scheduleToStart, Duration scheduleToClose,
                                          CapabilityRetryOptions retryOptions,
                                          AsyncCompletionPolicy asyncPolicy) {
        return addAsyncGroup(stageNames, timeout, taskQueue, scheduleToStart, scheduleToClose, retryOptions,
                asyncPolicy, "LAST_WINS");
    }

    public CapabilityPlanBuilder addAsyncGroup(List<String> stageNames, Duration timeout, String taskQueue,
                                          Duration scheduleToStart, Duration scheduleToClose,
                                          CapabilityRetryOptions retryOptions,
                                          AsyncCompletionPolicy asyncPolicy,
                                          String asyncOutputMergePolicyName) {
        return addAsyncGroup(stageNames, timeout, taskQueue, scheduleToStart, scheduleToClose, retryOptions, asyncPolicy, asyncOutputMergePolicyName, null);
    }

    public CapabilityPlanBuilder addAsyncGroup(List<String> stageNames, Duration timeout, String taskQueue,
                                          Duration scheduleToStart, Duration scheduleToClose,
                                          CapabilityRetryOptions retryOptions,
                                          AsyncCompletionPolicy asyncPolicy,
                                          String asyncOutputMergePolicyName,
                                          String stageBucketName) {
        List<CapabilityDefinition> definitions = new ArrayList<>();
        for (String name : stageNames) {
            definitions.add(CapabilityDefinition.builder()
                    .name(name)
                    .executionMode(CapabilityExecutionMode.ASYNC)
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
        groups.add(new CapabilityGroupSpec(definitions, asyncPolicy != null ? asyncPolicy : AsyncCompletionPolicy.ALL,
                asyncOutputMergePolicyName));
        return this;
    }

    /**
     * Add a conditional group (if/elseif/else): run condition activity first, then run one of the branches.
     * Condition plugin must write output key {@code branch} (Integer: 0=then, 1=first elseif, ..., n-1=else).
     */
    public CapabilityPlanBuilder addConditionalGroup(CapabilityDefinition conditionDefinition, List<List<CapabilityGroupSpec>> branches) {
        if (conditionDefinition == null || branches == null || branches.isEmpty()) {
            throw new IllegalArgumentException("conditionDefinition and non-empty branches required");
        }
        groups.add(new CapabilityGroupSpec(conditionDefinition, branches));
        return this;
    }

    public CapabilityPlan build() {
        return new CapabilityPlan(groups);
    }
}

