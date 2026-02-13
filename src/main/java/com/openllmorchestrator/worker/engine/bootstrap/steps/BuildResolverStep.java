package com.openllmorchestrator.worker.engine.bootstrap.steps;

import com.openllmorchestrator.worker.engine.bootstrap.BootstrapContext;
import com.openllmorchestrator.worker.engine.bootstrap.BootstrapStep;
import com.openllmorchestrator.worker.engine.stage.activity.ActivityRegistry;
import com.openllmorchestrator.worker.engine.stage.resolver.StageResolver;

/** Step: build activity registry and stage resolver from config and buckets. */
public final class BuildResolverStep implements BootstrapStep {
    @Override
    public void run(BootstrapContext ctx) {
        ActivityRegistry activityRegistry = ctx.getActivityRegistry();
        if (activityRegistry == null) {
            activityRegistry = ActivityRegistry.createDefault();
            ctx.setActivityRegistry(activityRegistry);
        }
        ctx.setResolver(new StageResolver(
                ctx.getConfig(), ctx.getPredefinedBucket(), ctx.getCustomBucket(), activityRegistry));
    }
}
