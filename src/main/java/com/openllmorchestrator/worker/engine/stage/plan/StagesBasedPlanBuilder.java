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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openllmorchestrator.worker.engine.config.EngineFileConfig;
import com.openllmorchestrator.worker.engine.config.pipeline.GroupConfig;
import com.openllmorchestrator.worker.engine.config.pipeline.MergePolicyConfig;
import com.openllmorchestrator.worker.engine.config.pipeline.PipelineSection;
import com.openllmorchestrator.worker.engine.config.pipeline.StageBlockConfig;
import com.openllmorchestrator.worker.engine.stage.AsyncCompletionPolicy;
import com.openllmorchestrator.worker.engine.stage.StageExecutionMode;
import com.openllmorchestrator.worker.engine.stage.StagePlanBuilder;
import com.openllmorchestrator.worker.engine.stage.StageRetryOptions;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds StagePlan from pipeline.stages: top-level stages, each with groups (sync/async recursive);
 * group children are activity names (plugin ids), each implemented by one StageHandler.
 */
public final class StagesBasedPlanBuilder {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private StagesBasedPlanBuilder() {}

    public static void build(EngineFileConfig fileConfig, StagePlanBuilder builder) {
        Map<String, PipelineSection> pipelines = fileConfig != null ? fileConfig.getPipelinesEffective() : null;
        if (pipelines != null && !pipelines.isEmpty()) {
            PipelineSection section = pipelines.containsKey("default") ? pipelines.get("default") : pipelines.values().iterator().next();
            build(fileConfig, section, builder);
        }
    }

    /** Build from a specific pipeline section (for named pipelines). */
    public static void build(EngineFileConfig fileConfig, PipelineSection section, StagePlanBuilder builder) {
        build(fileConfig, section, builder, null);
    }

    /** Build from a specific pipeline section; when allowedPluginNames is non-null, only those plugins may appear. */
    public static void build(EngineFileConfig fileConfig, PipelineSection section, StagePlanBuilder builder,
                             Set<String> allowedPluginNames) {
        if (fileConfig == null || section == null) {
            return;
        }
        List<StageBlockConfig> stages = section.getStages();
        if (stages == null || stages.isEmpty()) {
            return;
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
        for (StageBlockConfig stageBlock : stages) {
            if (stageBlock == null || stageBlock.getGroupsSafe().isEmpty()) {
                continue;
            }
            for (GroupConfig group : stageBlock.getGroupsSafe()) {
                processGroup(group, section, ctx, builder, 0);
            }
        }
    }

    private static void processGroup(GroupConfig group, PipelineSection section, PlanBuildContext ctx, StagePlanBuilder builder, int depth) {
        if (group == null) return;
        int effectiveMax = group.getMaxDepth() != null ? group.getMaxDepth() : ctx.getDefaultMaxGroupDepth();
        if (depth >= effectiveMax) {
            throw new IllegalStateException("Group recursion depth " + depth + " exceeds max " + effectiveMax);
        }
        int timeoutSeconds = group.getTimeoutSeconds() != null && group.getTimeoutSeconds() > 0
                ? group.getTimeoutSeconds()
                : ctx.getDefaultTimeoutSeconds();
        Duration timeout = Duration.ofSeconds(timeoutSeconds);
        Duration scheduleToStart = ActivityOptionsFromConfig.scheduleToStart(null, ctx);
        Duration scheduleToClose = ActivityOptionsFromConfig.scheduleToClose(null, ctx);
        StageRetryOptions retryOptions = ActivityOptionsFromConfig.retryOptions(null, ctx);

        if (group.isAsync()) {
            List<String> activityNames = flattenActivityNames(group, ctx);
            AsyncCompletionPolicy policy = group.getAsyncCompletionPolicy() != null && !group.getAsyncCompletionPolicy().isBlank()
                    ? AsyncCompletionPolicy.fromConfig(group.getAsyncCompletionPolicy())
                    : ctx.getDefaultAsyncPolicy();
            String mergePolicyName = resolveMergePolicyName(
                    group.getMergePolicy(), group.getAsyncOutputMergePolicy(),
                    section != null ? section.getMergePolicy() : null);
            builder.addAsyncGroup(
                    activityNames,
                    timeout,
                    ctx.getTaskQueue(),
                    scheduleToStart,
                    scheduleToClose,
                    retryOptions,
                    policy,
                    mergePolicyName
            );
        } else {
            for (Object child : group.getChildrenAsList()) {
                if (child instanceof String) {
                    String name = ((String) child).trim();
                    if (!name.isEmpty()) {
                        if (ctx.getAllowedPluginNames() != null && !ctx.getAllowedPluginNames().contains(name)) {
                            throw new IllegalStateException(
                                    "Plugin not allowed or incompatible: " + name
                                            + ". Add it to config.plugins and ensure contract compatibility.");
                        }
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
                    processGroup(nested, section, ctx, builder, depth + 1);
                }
            }
        }
    }

    private static String resolveMergePolicyName(MergePolicyConfig groupMergePolicy, String groupLegacy,
                                                  MergePolicyConfig sectionMergePolicy) {
        if (groupMergePolicy != null && groupMergePolicy.getName() != null && !groupMergePolicy.getName().isBlank()) {
            return groupMergePolicy.getName();
        }
        if (groupLegacy != null && !groupLegacy.isBlank()) {
            return groupLegacy;
        }
        if (sectionMergePolicy != null && sectionMergePolicy.getName() != null && !sectionMergePolicy.getName().isBlank()) {
            return sectionMergePolicy.getName();
        }
        return "LAST_WINS";
    }

    private static List<String> flattenActivityNames(GroupConfig group, PlanBuildContext ctx) {
        List<String> out = new ArrayList<>();
        for (Object child : group.getChildrenAsList()) {
            if (child instanceof String) {
                String name = ((String) child).trim();
                if (!name.isEmpty()) {
                    if (ctx.getAllowedPluginNames() != null && !ctx.getAllowedPluginNames().contains(name)) {
                        throw new IllegalStateException(
                                "Plugin not allowed or incompatible: " + name
                                        + ". Add it to config.plugins and ensure contract compatibility.");
                    }
                    out.add(name);
                }
            } else if (child instanceof Map) {
                GroupConfig nested = MAPPER.convertValue(child, GroupConfig.class);
                out.addAll(flattenActivityNames(nested, ctx));
            }
        }
        return out;
    }
}

