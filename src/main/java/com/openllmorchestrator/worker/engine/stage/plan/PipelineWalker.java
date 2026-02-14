package com.openllmorchestrator.worker.engine.stage.plan;

import com.openllmorchestrator.worker.engine.config.pipeline.NodeConfig;
import com.openllmorchestrator.worker.engine.stage.StagePlanBuilder;

/** Callback to process a child node when building the plan. */
public interface PipelineWalker {
    void processNode(NodeConfig node, PlanBuildContext ctx, StagePlanBuilder builder, int depth);
}
