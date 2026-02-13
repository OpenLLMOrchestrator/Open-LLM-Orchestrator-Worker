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
