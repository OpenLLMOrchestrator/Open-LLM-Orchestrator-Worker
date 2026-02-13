package com.openllmorchestrator.worker.engine.stage.plan;

import com.openllmorchestrator.worker.engine.config.pipeline.NodeConfig;
import com.openllmorchestrator.worker.engine.stage.StageExecutionMode;
import com.openllmorchestrator.worker.engine.stage.StagePlanBuilder;
import com.openllmorchestrator.worker.engine.stage.StageRetryOptions;

import java.time.Duration;

/** Processes STAGE nodes: adds one sync group. All options from config. */
public final class StageNodeProcessor implements NodeProcessor {
    @Override
    public boolean supports(NodeConfig node) {
        return node != null && node.isStage();
    }

    @Override
    public void process(NodeConfig node, PlanBuildContext ctx, StagePlanBuilder builder, PipelineWalker walker) {
        int timeout = node.getTimeoutSeconds() != null ? node.getTimeoutSeconds() : ctx.getDefaultTimeoutSeconds();
        builder.addSyncWithCustomConfig(
                node.getName(),
                StageExecutionMode.SYNC,
                Duration.ofSeconds(timeout),
                ctx.getTaskQueue(),
                ActivityOptionsFromConfig.scheduleToStart(node, ctx),
                ActivityOptionsFromConfig.scheduleToClose(node, ctx),
                ActivityOptionsFromConfig.retryOptions(node, ctx)
        );
    }
}
