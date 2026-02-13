package com.openllmorchestrator.worker.engine.stage.plan;

import com.openllmorchestrator.worker.engine.config.EngineFileConfig;
import com.openllmorchestrator.worker.engine.config.pipeline.NodeConfig;
import com.openllmorchestrator.worker.engine.config.pipeline.StageBlockConfig;
import com.openllmorchestrator.worker.engine.stage.StagePlan;
import com.openllmorchestrator.worker.engine.stage.StagePlanBuilder;

import java.util.List;

/** Builds StagePlan from config. Single responsibility: plan construction. */
public final class StagePlanFactory {
    private static final List<NodeProcessor> PROCESSORS = List.of(
            new StageNodeProcessor(),
            new GroupNodeProcessor()
    );

    private StagePlanFactory() {}

    public static StagePlan fromFileConfig(EngineFileConfig fileConfig) {
        StagePlanBuilder builder = StagePlan.builder();
        List<StageBlockConfig> stages = fileConfig.getPipeline() != null ? fileConfig.getPipeline().getStages() : null;
        if (stages != null && !stages.isEmpty()) {
            StagesBasedPlanBuilder.build(fileConfig, builder);
            return builder.build();
        }
        NodeConfig root = fileConfig.getPipeline().getRoot();
        if (root == null) {
            throw new IllegalStateException("Pipeline root is missing in config. Set pipeline.root or pipeline.stages.");
        }
        PlanBuildContext ctx = new PlanBuildContext(
                fileConfig.getPipeline().getDefaultTimeoutSeconds(),
                fileConfig.getWorker().getQueueName(),
                fileConfig.getActivity(),
                fileConfig.getPipeline().getDefaultAsyncCompletionPolicy()
        );
        DefaultPipelineWalker walker = new DefaultPipelineWalker();
        walker.processNode(root, ctx, builder);
        return builder.build();
    }

    private static class DefaultPipelineWalker implements PipelineWalker {
        @Override
        public void processNode(NodeConfig node, PlanBuildContext ctx, StagePlanBuilder builder) {
            for (NodeProcessor p : PROCESSORS) {
                if (p.supports(node)) {
                    p.process(node, ctx, builder, this);
                    return;
                }
            }
            throw new IllegalStateException("Unsupported pipeline node type: " + node.getType());
        }
    }
}
