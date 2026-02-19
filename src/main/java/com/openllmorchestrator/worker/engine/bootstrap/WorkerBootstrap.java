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
package com.openllmorchestrator.worker.engine.bootstrap;

import com.openllmorchestrator.worker.engine.bootstrap.steps.BuildActivityRegistryStep;
import com.openllmorchestrator.worker.engine.bootstrap.steps.BuildCompatiblePluginsStep;
import com.openllmorchestrator.worker.engine.bootstrap.steps.BuildFeatureHandlersStep;
import com.openllmorchestrator.worker.engine.bootstrap.steps.LoadDynamicPluginsStep;
import com.openllmorchestrator.worker.engine.bootstrap.steps.BuildPlanStep;
import com.openllmorchestrator.worker.engine.bootstrap.steps.BuildResolverStep;
import com.openllmorchestrator.worker.engine.bootstrap.steps.LoadConfigStep;
import com.openllmorchestrator.worker.engine.bootstrap.steps.SetRuntimeStep;
import com.openllmorchestrator.worker.engine.bootstrap.steps.ValidateConfigStep;
import com.openllmorchestrator.worker.engine.capability.bucket.CapabilityBucketFactory;
import com.openllmorchestrator.worker.engine.capability.custom.CustomCapabilityBucket;
import com.openllmorchestrator.worker.engine.config.EngineFileConfig;

import java.util.List;

/**
 * Runs bootstrap steps in a fixed hierarchy. Builds the execution tree (plans, resolver)
 * once; all outputs are stateless (no transactional or request-scoped data). Features
 * are added to the hierarchy at bootstrap time; at runtime they get a fair chance via
 * interceptors and feature flags during traversal.
 */
public final class WorkerBootstrap {
    /** Order: load config, build registries and resolver, validate, build plans, set runtime, build feature handlers. */
    private static final List<BootstrapStep> DEFAULT_STEPS = List.of(
            new LoadConfigStep(),
            new BuildActivityRegistryStep(),
            new LoadDynamicPluginsStep(),
            new BuildCompatiblePluginsStep(),
            new BuildResolverStep(),
            new ValidateConfigStep(),
            new BuildPlanStep(),
            new SetRuntimeStep(),
            new BuildFeatureHandlersStep()
    );

    private WorkerBootstrap() {}

    /** Bootstrap for the given task queue. Config is loaded per queue (Redis/DB/file keyed by queueName). */
    public static EngineFileConfig initialize(String queueName) {
        BootstrapContext ctx = new BootstrapContext();
        ctx.setQueueName(queueName != null && !queueName.isBlank() ? queueName : null);
        ctx.setEnvConfig(com.openllmorchestrator.worker.engine.config.env.EnvConfig.fromEnvironment());
        ctx.setPredefinedBucket(CapabilityBucketFactory.createPredefinedBucket());
        ctx.setCustomBucket(CapabilityBucketFactory.createCustomBucket());
        runSteps(ctx, DEFAULT_STEPS);
        return ctx.getConfig();
    }

    /** Backward compat: bootstrap using queue name from env (QUEUE_NAME). */
    public static EngineFileConfig initialize() {
        String queueName = com.openllmorchestrator.worker.engine.config.env.EnvConfig.fromEnvironment().getWorker().getQueueName();
        return initialize(queueName);
    }

    public static EngineFileConfig initializeWithCustomBucket(CustomCapabilityBucket customBucket) {
        if (customBucket == null) {
            throw new IllegalArgumentException("CustomCapabilityBucket must be non-null");
        }
        return initializeWithCustomBucket(null, customBucket);
    }

    /** Bootstrap for the given queue with custom capability bucket. */
    public static EngineFileConfig initializeWithCustomBucket(String queueName, CustomCapabilityBucket customBucket) {
        if (customBucket == null) {
            throw new IllegalArgumentException("CustomCapabilityBucket must be non-null");
        }
        BootstrapContext ctx = new BootstrapContext();
        ctx.setQueueName(queueName != null && !queueName.isBlank() ? queueName : null);
        ctx.setEnvConfig(com.openllmorchestrator.worker.engine.config.env.EnvConfig.fromEnvironment());
        ctx.setPredefinedBucket(CapabilityBucketFactory.createPredefinedBucket());
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

