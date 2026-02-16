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

import com.openllmorchestrator.worker.engine.config.pipeline.NodeConfig;
import com.openllmorchestrator.worker.engine.stage.StageExecutionMode;
import com.openllmorchestrator.worker.engine.stage.StagePlanBuilder;
import com.openllmorchestrator.worker.engine.stage.StageRetryOptions;

import java.time.Duration;

/** Processes STAGE nodes: adds one sync group. All options from config. */
public final class StageNodeProcessor implements NodeProcessor {
    @Override
    public boolean supports(NodeConfig node) {
        return node != null && node.isStage();
    }

    @Override
    public void process(NodeConfig node, PlanBuildContext ctx, StagePlanBuilder builder, PipelineWalker walker, int depth) {
        if (ctx.getAllowedPluginNames() != null && !ctx.getAllowedPluginNames().contains(node.getName())) {
            throw new IllegalStateException(
                    "Plugin not allowed or incompatible: " + node.getName()
                            + ". Add it to config.plugins and ensure contract compatibility.");
        }
        int timeout = node.getTimeoutSeconds() != null ? node.getTimeoutSeconds() : ctx.getDefaultTimeoutSeconds();
        builder.addSyncWithCustomConfig(
                node.getName(),
                StageExecutionMode.SYNC,
                Duration.ofSeconds(timeout),
                ctx.getTaskQueue(),
                ActivityOptionsFromConfig.scheduleToStart(node, ctx),
                ActivityOptionsFromConfig.scheduleToClose(node, ctx),
                ActivityOptionsFromConfig.retryOptions(node, ctx),
                ctx.getCurrentStageBucketName()
        );
    }
}

