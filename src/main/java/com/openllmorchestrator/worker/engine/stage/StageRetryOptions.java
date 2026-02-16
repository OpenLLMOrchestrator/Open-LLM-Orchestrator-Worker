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

import com.openllmorchestrator.worker.engine.config.activity.RetryPolicyConfig;
import lombok.Builder;
import lombok.Getter;

import java.util.Collections;
import java.util.List;

/** Runtime retry options for an activity. Built from config; no hardcoded values. */
@Getter
@Builder
public class StageRetryOptions {
    private final int maximumAttempts;
    private final int initialIntervalSeconds;
    private final double backoffCoefficient;
    private final int maximumIntervalSeconds;
    private final List<String> nonRetryableErrors;

    public List<String> getNonRetryableErrors() {
        if (nonRetryableErrors == null || nonRetryableErrors.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(nonRetryableErrors);
    }

    public static StageRetryOptions from(RetryPolicyConfig c) {
        if (c == null) return null;
        return StageRetryOptions.builder()
                .maximumAttempts(c.getMaximumAttempts())
                .initialIntervalSeconds(c.getInitialIntervalSeconds())
                .backoffCoefficient(c.getBackoffCoefficient())
                .maximumIntervalSeconds(c.getMaximumIntervalSeconds())
                .nonRetryableErrors(c.getNonRetryableErrors())
                .build();
    }
}

