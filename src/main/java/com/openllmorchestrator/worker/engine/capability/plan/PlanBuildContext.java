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
package com.openllmorchestrator.worker.engine.capability.plan;

import com.openllmorchestrator.worker.engine.config.activity.ActivityDefaultsConfig;
import com.openllmorchestrator.worker.engine.capability.AsyncCompletionPolicy;
import lombok.Getter;

import java.util.Set;

/** Context for building a stage plan from config. No hardcoded defaults. */
@Getter
public class PlanBuildContext {
    private final int defaultTimeoutSeconds;
    private final String taskQueue;
    private final ActivityDefaultsConfig activityDefaults;
    private final AsyncCompletionPolicy defaultAsyncPolicy;
    /** Max depth for GROUP recursion (default 5). */
    private final int defaultMaxGroupDepth;
    /** Current capability bucket name (e.g. RETRIEVAL, MODEL) when building from rootByStage; used for activity summary in UI. */
    private final String currentStageBucketName;
    /** When non-null, only these plugin names may appear in the plan (compatible plugins from bootstrap). */
    private final Set<String> allowedPluginNames;

    public PlanBuildContext(int defaultTimeoutSeconds, String taskQueue,
                            ActivityDefaultsConfig activityDefaults,
                            String defaultAsyncCompletionPolicy) {
        this(defaultTimeoutSeconds, taskQueue, activityDefaults, defaultAsyncCompletionPolicy, 5, null, null);
    }

    public PlanBuildContext(int defaultTimeoutSeconds, String taskQueue,
                            ActivityDefaultsConfig activityDefaults,
                            String defaultAsyncCompletionPolicy,
                            int defaultMaxGroupDepth) {
        this(defaultTimeoutSeconds, taskQueue, activityDefaults, defaultAsyncCompletionPolicy, defaultMaxGroupDepth, null, null);
    }

    public PlanBuildContext(int defaultTimeoutSeconds, String taskQueue,
                            ActivityDefaultsConfig activityDefaults,
                            String defaultAsyncCompletionPolicy,
                            int defaultMaxGroupDepth,
                            String currentStageBucketName) {
        this(defaultTimeoutSeconds, taskQueue, activityDefaults, defaultAsyncCompletionPolicy, defaultMaxGroupDepth, currentStageBucketName, null);
    }

    public PlanBuildContext(int defaultTimeoutSeconds, String taskQueue,
                            ActivityDefaultsConfig activityDefaults,
                            String defaultAsyncCompletionPolicy,
                            int defaultMaxGroupDepth,
                            String currentStageBucketName,
                            Set<String> allowedPluginNames) {
        this.defaultTimeoutSeconds = defaultTimeoutSeconds;
        this.taskQueue = taskQueue;
        this.activityDefaults = activityDefaults != null ? activityDefaults : new com.openllmorchestrator.worker.engine.config.activity.ActivityDefaultsConfig();
        this.defaultAsyncPolicy = AsyncCompletionPolicy.fromConfig(defaultAsyncCompletionPolicy);
        this.defaultMaxGroupDepth = defaultMaxGroupDepth > 0 ? defaultMaxGroupDepth : 5;
        this.currentStageBucketName = currentStageBucketName != null && !currentStageBucketName.isBlank() ? currentStageBucketName : null;
        this.allowedPluginNames = allowedPluginNames;
    }

    /** Returns a new context with the given stage bucket name (for activity summary in Temporal UI). */
    public PlanBuildContext withCurrentStageBucketName(String stageBucketName) {
        return new PlanBuildContext(
                defaultTimeoutSeconds, taskQueue, activityDefaults,
                defaultAsyncPolicy.name(), defaultMaxGroupDepth,
                stageBucketName, allowedPluginNames);
    }

    /** Returns a new context with the given allowed plugin names (for compatibility check during plan build). */
    public PlanBuildContext withAllowedPluginNames(Set<String> allowedPluginNames) {
        return new PlanBuildContext(
                defaultTimeoutSeconds, taskQueue, activityDefaults,
                defaultAsyncPolicy.name(), defaultMaxGroupDepth,
                currentStageBucketName, allowedPluginNames);
    }
}

