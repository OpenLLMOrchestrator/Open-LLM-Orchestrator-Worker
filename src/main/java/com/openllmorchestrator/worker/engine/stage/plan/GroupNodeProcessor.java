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

import com.openllmorchestrator.worker.engine.config.pipeline.ElseIfBranchNodeConfig;
import com.openllmorchestrator.worker.engine.config.pipeline.MergePolicyConfig;
import com.openllmorchestrator.worker.engine.config.pipeline.NodeConfig;
import com.openllmorchestrator.worker.engine.stage.AsyncCompletionPolicy;
import com.openllmorchestrator.worker.engine.stage.StageDefinition;
import com.openllmorchestrator.worker.engine.stage.StageExecutionMode;
import com.openllmorchestrator.worker.engine.stage.StageGroupSpec;
import com.openllmorchestrator.worker.engine.stage.StagePlan;
import com.openllmorchestrator.worker.engine.stage.StagePlanBuilder;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/** Processes GROUP nodes: SYNC = recurse children; ASYNC = one parallel group; conditional = if/elseif/else. Options from config. */
public final class GroupNodeProcessor implements NodeProcessor {
    @Override
    public boolean supports(NodeConfig node) {
        return node != null && node.isGroup();
    }

    @Override
    public void process(NodeConfig node, PlanBuildContext ctx, StagePlanBuilder builder, PipelineWalker walker, int depth) {
        int effectiveMax = node.getMaxDepth() != null ? node.getMaxDepth() : ctx.getDefaultMaxGroupDepth();
        if (depth >= effectiveMax) {
            throw new IllegalStateException("Group recursion depth " + depth + " exceeds max " + effectiveMax);
        }
        if (node.isConditional()) {
            processConditional(node, ctx, builder, walker, depth);
            return;
        }
        if ("ASYNC".equalsIgnoreCase(node.getExecutionMode())) {
            List<String> names = collectStageNames(node, ctx);
            int timeout = node.getTimeoutSeconds() != null ? node.getTimeoutSeconds() : ctx.getDefaultTimeoutSeconds();
            AsyncCompletionPolicy policy = node.getAsyncCompletionPolicy() != null && !node.getAsyncCompletionPolicy().isBlank()
                    ? AsyncCompletionPolicy.fromConfig(node.getAsyncCompletionPolicy())
                    : ctx.getDefaultAsyncPolicy();
            String mergePolicyName = resolveMergePolicyName(node.getMergePolicy(), node.getAsyncOutputMergePolicy());
            builder.addAsyncGroup(
                    names,
                    Duration.ofSeconds(timeout),
                    ctx.getTaskQueue(),
                    ActivityOptionsFromConfig.scheduleToStart(node, ctx),
                    ActivityOptionsFromConfig.scheduleToClose(node, ctx),
                    ActivityOptionsFromConfig.retryOptions(node, ctx),
                    policy,
                    mergePolicyName,
                    ctx.getCurrentStageBucketName()
            );
        } else {
            for (NodeConfig child : node.getChildren()) {
                walker.processNode(child, ctx, builder, depth + 1);
            }
        }
    }

    private static String resolveMergePolicyName(MergePolicyConfig mergePolicy, String legacyAsyncOutputMergePolicy) {
        if (mergePolicy != null && mergePolicy.getName() != null && !mergePolicy.getName().isBlank()) {
            return mergePolicy.getName();
        }
        return legacyAsyncOutputMergePolicy != null && !legacyAsyncOutputMergePolicy.isBlank() ? legacyAsyncOutputMergePolicy : "LAST_WINS";
    }

    private static List<String> collectStageNames(NodeConfig node, PlanBuildContext ctx) {
        List<String> names = new ArrayList<>();
        for (NodeConfig child : node.getChildren()) {
            if (child.isStage()) {
                String name = child.getName();
                if (ctx.getAllowedPluginNames() != null && !ctx.getAllowedPluginNames().contains(name)) {
                    throw new IllegalStateException(
                            "Plugin not allowed or incompatible: " + name
                                    + ". Add it to config.plugins and ensure contract compatibility.");
                }
                names.add(name);
            } else if (child.isGroup()) {
                names.addAll(collectStageNames(child, ctx));
            }
        }
        return names;
    }

    private void processConditional(NodeConfig node, PlanBuildContext ctx, StagePlanBuilder builder, PipelineWalker walker, int depth) {
        String conditionName = node.getCondition().trim();
        if (ctx.getAllowedPluginNames() != null && !ctx.getAllowedPluginNames().contains(conditionName)) {
            throw new IllegalStateException(
                    "Condition plugin not allowed or incompatible: " + conditionName
                            + ". Add it to config.plugins and ensure contract compatibility.");
        }
        int timeout = node.getTimeoutSeconds() != null ? node.getTimeoutSeconds() : ctx.getDefaultTimeoutSeconds();
        StageDefinition conditionDef = StageDefinition.builder()
                .name(conditionName)
                .executionMode(StageExecutionMode.SYNC)
                .group(0)
                .taskQueue(ctx.getTaskQueue())
                .timeout(Duration.ofSeconds(timeout))
                .scheduleToStartTimeout(ActivityOptionsFromConfig.scheduleToStart(node, ctx))
                .scheduleToCloseTimeout(ActivityOptionsFromConfig.scheduleToClose(node, ctx))
                .retryOptions(ActivityOptionsFromConfig.retryOptions(node, ctx))
                .stageBucketName("CONDITION")
                .build();
        List<List<StageGroupSpec>> branches = new ArrayList<>();
        branches.add(node.hasThenGroup()
                ? buildBranchSpecs(List.of(node.getThenGroup()), ctx, walker, depth)
                : buildBranchSpecs(node.getThenChildrenSafe(), ctx, walker, depth));
        for (ElseIfBranchNodeConfig elseif : node.getElseifBranchesSafe()) {
            branches.add(elseif.hasThenGroup()
                    ? buildBranchSpecs(List.of(elseif.getThenGroup()), ctx, walker, depth)
                    : buildBranchSpecs(elseif.getThenSafe(), ctx, walker, depth));
        }
        branches.add(node.hasElseGroup()
                ? buildBranchSpecs(List.of(node.getElseGroup()), ctx, walker, depth)
                : buildBranchSpecs(node.getElseChildrenSafe(), ctx, walker, depth));
        builder.addConditionalGroup(conditionDef, branches);
    }

    private static List<StageGroupSpec> buildBranchSpecs(List<NodeConfig> nodes, PlanBuildContext ctx,
                                                         PipelineWalker walker, int depth) {
        StagePlanBuilder branchBuilder = StagePlan.builder();
        for (NodeConfig n : nodes) {
            walker.processNode(n, ctx, branchBuilder, depth + 1);
        }
        return branchBuilder.build().getGroups();
    }
}

