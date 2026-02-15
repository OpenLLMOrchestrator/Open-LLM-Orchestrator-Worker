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

import lombok.Builder;
import lombok.Getter;

import java.time.Duration;

/** One stage/activity in the plan. All timeouts and retry from config. Immutable; no request-scoped data. */
@Getter
@Builder
public class StageDefinition {
    private final String name;
    private final String taskQueue;
    private final StageExecutionMode executionMode;
    private final int group;
    /** Start-to-close (activity execution) timeout. */
    private final Duration timeout;
    /** Optional schedule-to-start timeout. */
    private final Duration scheduleToStartTimeout;
    /** Optional schedule-to-close timeout. */
    private final Duration scheduleToCloseTimeout;
    /** Optional retry policy; null = use default from config. */
    private final StageRetryOptions retryOptions;
}
