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
