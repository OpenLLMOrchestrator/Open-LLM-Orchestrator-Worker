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
