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

import com.openllmorchestrator.worker.engine.config.pipeline.MergePolicyConfig;
import com.openllmorchestrator.worker.engine.config.pipeline.NodeConfig;
import com.openllmorchestrator.worker.engine.stage.AsyncCompletionPolicy;
import com.openllmorchestrator.worker.engine.stage.StagePlanBuilder;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/** Processes GROUP nodes: SYNC = recurse children; ASYNC = one parallel group. Options from config. */
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
        if ("ASYNC".equalsIgnoreCase(node.getExecutionMode())) {
            List<String> names = collectStageNames(node);
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
                    mergePolicyName
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

    private static List<String> collectStageNames(NodeConfig node) {
        List<String> names = new ArrayList<>();
        for (NodeConfig child : node.getChildren()) {
            if (child.isStage()) {
                names.add(child.getName());
            } else if (child.isGroup()) {
                names.addAll(collectStageNames(child));
            }
        }
        return names;
    }
}
