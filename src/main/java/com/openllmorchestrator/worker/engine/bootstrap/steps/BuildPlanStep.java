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
package com.openllmorchestrator.worker.engine.bootstrap.steps;

import com.openllmorchestrator.worker.engine.bootstrap.BootstrapContext;
import com.openllmorchestrator.worker.engine.bootstrap.BootstrapStep;
import com.openllmorchestrator.worker.engine.config.pipeline.PipelineSection;
import com.openllmorchestrator.worker.engine.kernel.merge.MergePolicyConfigApplicator;
import com.openllmorchestrator.worker.engine.stage.StagePlan;
import com.openllmorchestrator.worker.engine.stage.plan.StagePlanFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Step: build execution hierarchy (stage plans) once from config.
 * When config has named pipelines, builds one plan per name; otherwise builds single "default" plan.
 * Plans are immutable and reused for the container lifecycle. Only plugins in the compatible
 * registry (from bootstrap compatibility check) are allowed in static pipeline structure.
 */
public final class BuildPlanStep implements BootstrapStep {
    @Override
    public void run(BootstrapContext ctx) {
        MergePolicyConfigApplicator.apply(ctx.getConfig());
        Map<String, PipelineSection> effective = ctx.getConfig().getPipelinesEffective();
        if (effective.isEmpty()) {
            throw new IllegalStateException("No pipeline config. Set pipelines with at least one pipeline in config.");
        }
        Set<String> allowedPluginNames = ctx.getCompatibleActivityRegistry() != null
                ? ctx.getCompatibleActivityRegistry().registeredNames()
                : null;
        Map<String, StagePlan> plans = new LinkedHashMap<>();
        for (Map.Entry<String, PipelineSection> e : effective.entrySet()) {
            String name = e.getKey();
            PipelineSection section = e.getValue();
            plans.put(name, StagePlanFactory.fromPipelineSection(ctx.getConfig(), section, allowedPluginNames));
        }
        ctx.setPlans(plans);
        if (plans.containsKey("default")) {
            ctx.setPlan(plans.get("default"));
        }
    }
}

