/*
 * Copyright 2026 Open LLM Orchestrator contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.openllmorchestrator.worker.engine.stage.plan;

import com.openllmorchestrator.worker.engine.config.EngineFileConfig;
import com.openllmorchestrator.worker.engine.config.FeatureFlag;
import com.openllmorchestrator.worker.engine.config.pipeline.NodeConfig;
import com.openllmorchestrator.worker.engine.config.pipeline.PipelineSection;
import com.openllmorchestrator.worker.engine.config.pipeline.StageBlockConfig;
import com.openllmorchestrator.worker.engine.stage.StagePlan;
import com.openllmorchestrator.worker.engine.stage.StagePlanBuilder;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** Builds StagePlan from config. Single responsibility: plan construction. */
public final class


































StagePlanFactory {
    private static final List<NodeProcessor> PROCESSORS = List.of(
            new StageNodeProcessor(),
            new GroupNodeProcessor()
    );

    private StagePlanFactory() {}

    /** Build a single plan from the first pipeline in config (e.g. for tests). Prefer "default" if present. */
    public static StagePlan fromFileConfig(EngineFileConfig fileConfig) {
        Map<String, PipelineSection> pipelines = fileConfig.getPipelinesEffective();
        if (pipelines == null || pipelines.isEmpty()) {
            throw new IllegalStateException("No pipeline config. Set pipelines in config (at least one required).");
        }
        PipelineSection section = pipelines.containsKey("default") ? pipelines.get("default") : pipelines.values().iterator().next();
        return fromPipelineSection(fileConfig, section);
    }

    /** Build a single plan from a given pipeline section (for named pipelines). */
    public static StagePlan fromPipelineSection(EngineFileConfig fileConfig, PipelineSection section) {
        return fromPipelineSection(fileConfig, section, null);
    }

    /**
     * Build a single plan from a given pipeline section. When allowedPluginNames is non-null,
     * only those plugin names may appear in the plan (compatible plugins from bootstrap).
     */
    public static StagePlan fromPipelineSection(EngineFileConfig fileConfig, PipelineSection section,
                                                Set<String> allowedPluginNames) {
        if (fileConfig == null || section == null) {
            throw new IllegalArgumentException("fileConfig and section must be non-null");
        }
        StagePlanBuilder builder = StagePlan.builder();
        List<StageBlockConfig> stages = section.getStages();
        if (stages != null && !stages.isEmpty()) {
            StagesBasedPlanBuilder.build(fileConfig, section, builder, allowedPluginNames);
            return builder.build();
        }
        Map<String, NodeConfig> rootByStage = section.getRootByStage();
        if (rootByStage != null && !rootByStage.isEmpty()) {
            buildFromRootByStage(fileConfig, section, rootByStage, builder, allowedPluginNames);
            return builder.build();
        }
        NodeConfig root = section.getRoot();
        if (root == null) {
            throw new IllegalStateException("Pipeline root is missing. Set root (stages map or single GROUP tree) or stages for this pipeline.");
        }
        int defaultMaxDepth = section.getDefaultMaxGroupDepth() > 0 ? section.getDefaultMaxGroupDepth() : 5;
        PlanBuildContext ctx = new PlanBuildContext(
                section.getDefaultTimeoutSeconds(),
                fileConfig.getWorker().getQueueName(),
                fileConfig.getActivity(),
                section.getDefaultAsyncCompletionPolicy(),
                defaultMaxDepth,
                null,
                allowedPluginNames
        );
        DefaultPipelineWalker walker = new DefaultPipelineWalker();
        walker.processNode(root, ctx, builder, 0);
        return builder.build();
    }

    /**
     * Build plan from rootByStage: only stages present in the map are included,
     * in the order defined by config stageOrder (or predefined order in code).
     */
    private static void buildFromRootByStage(EngineFileConfig fileConfig, PipelineSection section,
                                             Map<String, NodeConfig> rootByStage, StagePlanBuilder builder,
                                             Set<String> allowedPluginNames) {
        List<String> stageOrder = fileConfig.getFeatureFlagsEffective().isEnabled(FeatureFlag.EXECUTION_GRAPH)
                ? fileConfig.getExecutionGraphEffective().topologicalOrder()
                : fileConfig.getStageOrderEffective();
        int defaultMaxDepth = section.getDefaultMaxGroupDepth() > 0 ? section.getDefaultMaxGroupDepth() : 5;
        PlanBuildContext ctx = new PlanBuildContext(
                section.getDefaultTimeoutSeconds(),
                fileConfig.getWorker().getQueueName(),
                fileConfig.getActivity(),
                section.getDefaultAsyncCompletionPolicy(),
                defaultMaxDepth,
                null,
                allowedPluginNames
        );
        DefaultPipelineWalker walker = new DefaultPipelineWalker();
        for (String stageName : stageOrder) {
            NodeConfig groupNode = rootByStage.get(stageName);
            if (groupNode == null) {
                continue; // not defined → skip
            }
            if (!groupNode.isGroup()) {
                throw new IllegalStateException("Pipeline rootByStage['" + stageName + "'] must be a GROUP, got: " + groupNode.getType());
            }
            PlanBuildContext ctxForStage = ctx.withCurrentStageBucketName(stageName);
            walker.processNode(groupNode, ctxForStage, builder, 0);
        }
    }

    private static class DefaultPipelineWalker implements PipelineWalker {
        @Override
        public void processNode(NodeConfig node, PlanBuildContext ctx, StagePlanBuilder builder, int depth) {
            for (NodeProcessor p : PROCESSORS) {
                if (p.supports(node)) {
                    p.process(node, ctx, builder, this, depth);
                    return;
                }
            }
            throw new IllegalStateException("Unsupported pipeline node type: " + node.getType());
        }
    }
}

