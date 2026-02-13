package com.openllmorchestrator.worker.engine.config.activity;

import lombok.Getter;

/** Default activity options: timeouts and retry policy. No hardcoded values in engine. */
@Getter
public class ActivityDefaultsConfig {
    private ActivityTimeoutsConfig defaultTimeouts = new ActivityTimeoutsConfig();
    private RetryPolicyConfig retryPolicy = new RetryPolicyConfig();
}
