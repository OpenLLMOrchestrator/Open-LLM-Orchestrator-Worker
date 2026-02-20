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
package com.openllmorchestrator.worker.engine.capability.plan;

import com.openllmorchestrator.worker.engine.config.EngineConfigRuntime;
import com.openllmorchestrator.worker.engine.config.EngineFileConfig;
import com.openllmorchestrator.worker.engine.config.FeatureFlag;
import com.openllmorchestrator.worker.engine.config.pipeline.NodeConfig;
import com.openllmorchestrator.worker.engine.config.pipeline.PipelineSection;
import com.openllmorchestrator.worker.engine.config.pipeline.CapabilityBlockConfig;
import com.openllmorchestrator.worker.engine.capability.CapabilityPlan;
import com.openllmorchestrator.worker.engine.capability.CapabilityPlanBuilder;
import com.openllmorchestrator.worker.engine.capability.ExecutionTreeNode;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.TreeSet;

/** Builds CapabilityPlan from config. Single responsibility: plan construction. */
public final class


































CapabilityPlanFactory {
    private static final List<NodeProcessor> PROCESSORS = List.of(
            new PluginNodeProcessor(),
            new GroupNodeProcessor()
    );

    private CapabilityPlanFactory() {}

    /** Build a single plan from the first pipeline in config (e.g. for tests). Prefer "default" if present. */
    public static CapabilityPlan fromFileConfig(EngineFileConfig fileConfig) {
        Map<String, PipelineSection> pipelines = fileConfig.getPipelinesEffective();
        if (pipelines == null || pipelines.isEmpty()) {
            throw new IllegalStateException("No pipeline config. Set pipelines in config (at least one required).");
        }
        PipelineSection section = pipelines.containsKey("default") ? pipelines.get("default") : pipelines.values().iterator().next();
        return fromPipelineSection(fileConfig, section);
    }

    /** Build a single plan from a given pipeline section (for named pipelines). */
    public static CapabilityPlan fromPipelineSection(EngineFileConfig fileConfig, PipelineSection section) {
        return fromPipelineSection(fileConfig, section, null);
    }

    /**
     * Build a single plan from a given pipeline section. When allowedPluginNames is non-null,
     * only those plugin names may appear in the plan (compatible plugins from bootstrap).
     */
    public static CapabilityPlan fromPipelineSection(EngineFileConfig fileConfig, PipelineSection section,
                                                Set<String> allowedPluginNames) {
        if (fileConfig == null || section == null) {
            throw new IllegalArgumentException("fileConfig and section must be non-null");
        }
        CapabilityPlanBuilder builder = CapabilityPlan.builder();
        List<CapabilityBlockConfig> capabilities = section.getCapabilities();
        if (capabilities != null && !capabilities.isEmpty()) {
            CapabilitiesBasedPlanBuilder.build(fileConfig, section, builder, allowedPluginNames);
            return builder.build();
        }
        Map<String, NodeConfig> rootByCapability = section.getRootByCapability();
        if (rootByCapability != null && !rootByCapability.isEmpty()) {
            buildFromRootByCapability(fileConfig, section, rootByCapability, builder, allowedPluginNames);
            return builder.build();
        }
        NodeConfig root = section.getRoot();
        if (root == null) {
            throw new IllegalStateException("Pipeline root is missing. Set root (capabilities map or single GROUP tree) or stages for this pipeline.");
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
     * Build plan from rootByCapability: only capabilities present in the map are included,
     * in the order defined by config capabilityOrder (or predefined order in code).
     */
    private static void buildFromRootByCapability(EngineFileConfig fileConfig, PipelineSection section,
                                             Map<String, NodeConfig> rootByCapability, CapabilityPlanBuilder builder,
                                             Set<String> allowedPluginNames) {
        List<String> capabilityOrder = EngineConfigRuntime.getFeatureFlagsEffective(fileConfig).isEnabled(FeatureFlag.EXECUTION_GRAPH)
                ? EngineConfigRuntime.getExecutionGraphEffective(fileConfig).topologicalOrder()
                : EngineConfigRuntime.getCapabilityOrderEffective(fileConfig);
        // Include every capability present in rootByCapability; order by capabilityOrder first, then any remaining by name
        List<String> capabilityNamesToProcess = capabilityOrderForRootByCapability(capabilityOrder, rootByCapability);
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
        ExecutionTreeBuilder treeBuilder = new ExecutionTreeBuilder();
        for (String capabilityName : capabilityNamesToProcess) {
            NodeConfig groupNode = rootByCapability.get(capabilityName);
            if (groupNode == null) {
                continue; // defensive: skip if not in map
            }
            if (!groupNode.isGroup()) {
                throw new IllegalStateException("Pipeline rootByCapability['" + capabilityName + "'] must be a GROUP, got: " + groupNode.getType());
            }
            treeBuilder.startCapability(capabilityName);
            PlanBuildContext ctxForCapability = ctx.withCurrentCapabilityBucketName(capabilityName);
            walker.processNode(groupNode, ctxForCapability, builder, 0, treeBuilder);
            treeBuilder.endCapability();
        }
        builder.executionTree(treeBuilder.getRoots(), treeBuilder.getCapabilityNodeIds());
    }

    /**
     * Build ordered list of capability names: those from capabilityOrder that exist in rootByCapability (in that order),
     * then any other keys from rootByCapability that were not in capabilityOrder (sorted for determinism).
     * Ensures no capability defined in rootByCapability is omitted (e.g. RETRIEVAL when capabilityOrder only lists MODEL, POST_PROCESS).
     */
    private static List<String> capabilityOrderForRootByCapability(List<String> capabilityOrder,
                                                                   Map<String, NodeConfig> rootByCapability) {
        Set<String> inRoot = rootByCapability.keySet();
        LinkedHashSet<String> ordered = new LinkedHashSet<>();
        if (capabilityOrder != null) {
            for (String name : capabilityOrder) {
                if (inRoot.contains(name)) {
                    ordered.add(name);
                }
            }
        }
        TreeSet<String> remaining = new TreeSet<>(inRoot);
        remaining.removeAll(ordered);
        ordered.addAll(remaining);
        return new ArrayList<>(ordered);
    }

    private static class DefaultPipelineWalker implements PipelineWalker {
        @Override
        public void processNode(NodeConfig node, PlanBuildContext ctx, CapabilityPlanBuilder builder, int depth) {
            processNode(node, ctx, builder, depth, null);
        }

        @Override
        public void processNode(NodeConfig node, PlanBuildContext ctx, CapabilityPlanBuilder builder, int depth,
                               ExecutionTreeBuilder treeBuilder) {
            for (NodeProcessor p : PROCESSORS) {
                if (p.supports(node)) {
                    p.process(node, ctx, builder, this, depth, treeBuilder);
                    return;
                }
            }
            throw new IllegalStateException("Unsupported pipeline node type: " + node.getType());
        }
    }
}

