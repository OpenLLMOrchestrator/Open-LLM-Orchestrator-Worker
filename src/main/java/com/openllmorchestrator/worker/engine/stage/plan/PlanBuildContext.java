package com.openllmorchestrator.worker.engine.stage.plan;

import com.openllmorchestrator.worker.engine.config.activity.ActivityDefaultsConfig;
import com.openllmorchestrator.worker.engine.stage.AsyncCompletionPolicy;
import lombok.Getter;

/** Context for building a stage plan from config. No hardcoded defaults. */
@Getter
public class PlanBuildContext {
    private final int defaultTimeoutSeconds;
    private final String taskQueue;
    private final ActivityDefaultsConfig activityDefaults;
    private final AsyncCompletionPolicy defaultAsyncPolicy;

    public PlanBuildContext(int defaultTimeoutSeconds, String taskQueue,
                            ActivityDefaultsConfig activityDefaults,
                            String defaultAsyncCompletionPolicy) {
        this.defaultTimeoutSeconds = defaultTimeoutSeconds;
        this.taskQueue = taskQueue;
        this.activityDefaults = activityDefaults != null ? activityDefaults : new com.openllmorchestrator.worker.engine.config.activity.ActivityDefaultsConfig();
        this.defaultAsyncPolicy = AsyncCompletionPolicy.fromConfig(defaultAsyncCompletionPolicy);
    }
}
