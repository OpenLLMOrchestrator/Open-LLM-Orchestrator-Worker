package com.openllmorchestrator.worker.engine.bootstrap;

import com.openllmorchestrator.worker.engine.bootstrap.steps.BuildPlanStep;
import com.openllmorchestrator.worker.engine.bootstrap.steps.BuildResolverStep;
import com.openllmorchestrator.worker.engine.bootstrap.steps.LoadConfigStep;
import com.openllmorchestrator.worker.engine.bootstrap.steps.SetRuntimeStep;
import com.openllmorchestrator.worker.engine.bootstrap.steps.ValidateConfigStep;
import com.openllmorchestrator.worker.engine.config.EngineFileConfig;
import com.openllmorchestrator.worker.engine.stage.custom.CustomStageBucket;
import com.openllmorchestrator.worker.engine.stage.bucket.StageBucketFactory;
import com.openllmorchestrator.worker.engine.stage.predefined.PredefinedPluginBucket;

import java.util.List;

/** Runs bootstrap steps. Single responsibility: orchestrate startup. */
public final class WorkerBootstrap {
    private static final List<BootstrapStep> DEFAULT_STEPS = List.of(
            new LoadConfigStep(),
            new BuildResolverStep(),
            new ValidateConfigStep(),
            new BuildPlanStep(),
            new SetRuntimeStep()
    );

    private WorkerBootstrap() {}

    public static EngineFileConfig initialize() {
        BootstrapContext ctx = new BootstrapContext();
        ctx.setEnvConfig(com.openllmorchestrator.worker.engine.config.env.EnvConfig.fromEnvironment());
        ctx.setPredefinedBucket(StageBucketFactory.createPredefinedBucket());
        ctx.setCustomBucket(StageBucketFactory.createCustomBucket());
        runSteps(ctx, DEFAULT_STEPS);
        return ctx.getConfig();
    }

    public static EngineFileConfig initializeWithCustomBucket(CustomStageBucket customBucket) {
        if (customBucket == null) {
            throw new IllegalArgumentException("CustomStageBucket must be non-null");
        }
        BootstrapContext ctx = new BootstrapContext();
        ctx.setPredefinedBucket(StageBucketFactory.createPredefinedBucket());
        ctx.setCustomBucket(customBucket);
        runSteps(ctx, DEFAULT_STEPS);
        return ctx.getConfig();
    }

    private static void runSteps(BootstrapContext ctx, List<BootstrapStep> steps) {
        for (BootstrapStep step : steps) {
            step.run(ctx);
        }
    }
}
