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
import com.openllmorchestrator.worker.engine.config.activity.RetryPolicyConfig;
import com.openllmorchestrator.worker.engine.config.pipeline.NodeConfig;
import com.openllmorchestrator.worker.engine.capability.CapabilityRetryOptions;

import java.time.Duration;

/** Builds activity timeouts and retry from config + optional node overrides. */
final class ActivityOptionsFromConfig {

    private ActivityOptionsFromConfig() {}

    static Duration scheduleToStart(NodeConfig node, PlanBuildContext ctx) {
        Integer seconds = node != null && node.getScheduleToStartSeconds() != null
                ? node.getScheduleToStartSeconds()
                : (ctx.getActivityDefaults().getDefaultTimeouts() != null
                ? ctx.getActivityDefaults().getDefaultTimeouts().getScheduleToStartSeconds()
                : null);
        return seconds != null && seconds > 0 ? Duration.ofSeconds(seconds) : null;
    }

    static Duration scheduleToClose(NodeConfig node, PlanBuildContext ctx) {
        Integer seconds = node != null && node.getScheduleToCloseSeconds() != null
                ? node.getScheduleToCloseSeconds()
                : (ctx.getActivityDefaults().getDefaultTimeouts() != null
                ? ctx.getActivityDefaults().getDefaultTimeouts().getScheduleToCloseSeconds()
                : null);
        return seconds != null && seconds > 0 ? Duration.ofSeconds(seconds) : null;
    }

    static CapabilityRetryOptions retryOptions(NodeConfig node, PlanBuildContext ctx) {
        RetryPolicyConfig c = node != null && node.getRetryPolicy() != null
                ? node.getRetryPolicy()
                : (ctx.getActivityDefaults().getRetryPolicy() != null
                ? ctx.getActivityDefaults().getRetryPolicy()
                : null);
        return fromRetryPolicyConfig(c);
    }

    /** Build CapabilityRetryOptions from engine-config RetryPolicyConfig (engine-model has no config dependency). */
    static CapabilityRetryOptions fromRetryPolicyConfig(RetryPolicyConfig c) {
        if (c == null) return null;
        return CapabilityRetryOptions.builder()
                .maximumAttempts(c.getMaximumAttempts())
                .initialIntervalSeconds(c.getInitialIntervalSeconds())
                .backoffCoefficient(c.getBackoffCoefficient())
                .maximumIntervalSeconds(c.getMaximumIntervalSeconds())
                .nonRetryableErrors(c.getNonRetryableErrors())
                .build();
    }
}

