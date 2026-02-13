package com.openllmorchestrator.worker.engine.stage.plan;

import com.openllmorchestrator.worker.engine.config.activity.ActivityDefaultsConfig;
import com.openllmorchestrator.worker.engine.config.activity.RetryPolicyConfig;
import com.openllmorchestrator.worker.engine.config.pipeline.NodeConfig;
import com.openllmorchestrator.worker.engine.stage.StageRetryOptions;

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

    static StageRetryOptions retryOptions(NodeConfig node, PlanBuildContext ctx) {
        RetryPolicyConfig c = node != null && node.getRetryPolicy() != null
                ? node.getRetryPolicy()
                : (ctx.getActivityDefaults().getRetryPolicy() != null
                ? ctx.getActivityDefaults().getRetryPolicy()
                : null);
        return StageRetryOptions.from(c);
    }
}
