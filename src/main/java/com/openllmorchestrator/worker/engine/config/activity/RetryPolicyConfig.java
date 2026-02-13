package com.openllmorchestrator.worker.engine.config.activity;

import lombok.Getter;

import java.util.Collections;
import java.util.List;

/** Activity retry policy from config. No hardcoded retries in engine. */
@Getter
public class RetryPolicyConfig {
    private int maximumAttempts = 3;
    private int initialIntervalSeconds = 1;
    private double backoffCoefficient = 2.0;
    private int maximumIntervalSeconds = 60;
    private List<String> nonRetryableErrors;

    public List<String> getNonRetryableErrors() {
        return nonRetryableErrors != null ? nonRetryableErrors : Collections.emptyList();
    }
}
