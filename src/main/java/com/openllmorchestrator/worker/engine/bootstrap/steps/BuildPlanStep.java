package com.openllmorchestrator.worker.engine.bootstrap.steps;

import com.openllmorchestrator.worker.engine.bootstrap.BootstrapContext;
import com.openllmorchestrator.worker.engine.bootstrap.BootstrapStep;
import com.openllmorchestrator.worker.engine.config.pipeline.PipelineSection;
import com.openllmorchestrator.worker.engine.kernel.merge.MergePolicyConfigApplicator;
import com.openllmorchestrator.worker.engine.stage.StagePlan;
import com.openllmorchestrator.worker.engine.stage.plan.StagePlanFactory;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Step: build execution hierarchy (stage plans) once from config.
 * When config has named pipelines, builds one plan per name; otherwise builds single "default" plan.
 * Plans are immutable and reused for the container lifecycle.
 */
public final class BuildPlanStep implements BootstrapStep {
    @Override
    public void run(BootstrapContext ctx) {
        MergePolicyConfigApplicator.apply(ctx.getConfig());
        Map<String, PipelineSection> effective = ctx.getConfig().getPipelinesEffective();
        if (effective.isEmpty()) {
            throw new IllegalStateException("No pipeline config. Set pipelines with at least one pipeline in config.");
        }
        Map<String, StagePlan> plans = new LinkedHashMap<>();
        for (Map.Entry<String, PipelineSection> e : effective.entrySet()) {
            String name = e.getKey();
            PipelineSection section = e.getValue();
            plans.put(name, StagePlanFactory.fromPipelineSection(ctx.getConfig(), section));
        }
        ctx.setPlans(plans);
        if (plans.containsKey("default")) {
            ctx.setPlan(plans.get("default"));
        }
    }
}
