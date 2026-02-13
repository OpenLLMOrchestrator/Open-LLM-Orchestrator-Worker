package com.openllmorchestrator.worker.engine.stage.plan;

import com.openllmorchestrator.worker.engine.config.pipeline.NodeConfig;
import com.openllmorchestrator.worker.engine.stage.StagePlanBuilder;

/** Processes one node type. Add new node types by adding implementations (OCP). */
public interface NodeProcessor {
    boolean supports(NodeConfig node);

    void process(NodeConfig node, PlanBuildContext ctx, StagePlanBuilder builder, PipelineWalker walker);
}
