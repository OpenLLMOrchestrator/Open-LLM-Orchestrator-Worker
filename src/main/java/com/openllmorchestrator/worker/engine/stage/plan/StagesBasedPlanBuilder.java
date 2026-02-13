package com.openllmorchestrator.worker.engine.stage.plan;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openllmorchestrator.worker.engine.config.EngineFileConfig;
import com.openllmorchestrator.worker.engine.config.pipeline.GroupConfig;
import com.openllmorchestrator.worker.engine.config.pipeline.StageBlockConfig;
import com.openllmorchestrator.worker.engine.kernel.merge.AsyncOutputMergePolicy;
import com.openllmorchestrator.worker.engine.stage.AsyncCompletionPolicy;
import com.openllmorchestrator.worker.engine.stage.StageExecutionMode;
import com.openllmorchestrator.worker.engine.stage.StagePlanBuilder;
import com.openllmorchestrator.worker.engine.stage.StageRetryOptions;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Builds StagePlan from pipeline.stages: top-level stages, each with groups (sync/async recursive);
 * group children are activity names (plugin ids), each implemented by one StageHandler.
 */
public final class StagesBasedPlanBuilder {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private StagesBasedPlanBuilder() {}

    public static void build(EngineFileConfig fileConfig, StagePlanBuilder builder) {
        if (fileConfig == null || fileConfig.getPipeline() == null) {
            return;
        }
        List<StageBlockConfig> stages = fileConfig.getPipeline().getStages();
        if (stages == null || stages.isEmpty()) {
            return;
        }
        PlanBuildContext ctx = new PlanBuildContext(
                fileConfig.getPipeline().getDefaultTimeoutSeconds(),
                fileConfig.getWorker().getQueueName(),
                fileConfig.getActivity(),
                fileConfig.getPipeline().getDefaultAsyncCompletionPolicy()
        );
        for (StageBlockConfig stageBlock : stages) {
            if (stageBlock == null || stageBlock.getGroupsSafe().isEmpty()) {
                continue;
            }
            for (GroupConfig group : stageBlock.getGroupsSafe()) {
                processGroup(group, ctx, builder);
            }
        }
    }

    private static void processGroup(GroupConfig group, PlanBuildContext ctx, StagePlanBuilder builder) {
        if (group == null) return;
        int timeoutSeconds = group.getTimeoutSeconds() != null && group.getTimeoutSeconds() > 0
                ? group.getTimeoutSeconds()
                : ctx.getDefaultTimeoutSeconds();
        Duration timeout = Duration.ofSeconds(timeoutSeconds);
        Duration scheduleToStart = ActivityOptionsFromConfig.scheduleToStart(null, ctx);
        Duration scheduleToClose = ActivityOptionsFromConfig.scheduleToClose(null, ctx);
        StageRetryOptions retryOptions = ActivityOptionsFromConfig.retryOptions(null, ctx);

        if (group.isAsync()) {
            List<String> activityNames = flattenActivityNames(group);
            AsyncCompletionPolicy policy = group.getAsyncCompletionPolicy() != null && !group.getAsyncCompletionPolicy().isBlank()
                    ? AsyncCompletionPolicy.fromConfig(group.getAsyncCompletionPolicy())
                    : ctx.getDefaultAsyncPolicy();
            AsyncOutputMergePolicy outputMerge = group.getAsyncOutputMergePolicy() != null && !group.getAsyncOutputMergePolicy().isBlank()
                    ? AsyncOutputMergePolicy.fromConfig(group.getAsyncOutputMergePolicy())
                    : null;
            builder.addAsyncGroup(
                    activityNames,
                    timeout,
                    ctx.getTaskQueue(),
                    scheduleToStart,
                    scheduleToClose,
                    retryOptions,
                    policy,
                    outputMerge
            );
        } else {
            for (Object child : group.getChildrenAsList()) {
                if (child instanceof String) {
                    String name = ((String) child).trim();
                    if (!name.isEmpty()) {
                        builder.addSyncWithCustomConfig(
                                name,
                                StageExecutionMode.SYNC,
                                timeout,
                                ctx.getTaskQueue(),
                                scheduleToStart,
                                scheduleToClose,
                                retryOptions
                        );
                    }
                } else if (child instanceof Map) {
                    GroupConfig nested = MAPPER.convertValue(child, GroupConfig.class);
                    processGroup(nested, ctx, builder);
                }
            }
        }
    }

    private static List<String> flattenActivityNames(GroupConfig group) {
        List<String> out = new ArrayList<>();
        for (Object child : group.getChildrenAsList()) {
            if (child instanceof String) {
                String name = ((String) child).trim();
                if (!name.isEmpty()) {
                    out.add(name);
                }
            } else if (child instanceof Map) {
                GroupConfig nested = MAPPER.convertValue(child, GroupConfig.class);
                out.addAll(flattenActivityNames(nested));
            }
        }
        return out;
    }
}
