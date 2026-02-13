package com.openllmorchestrator.worker.engine.runtime;

import com.openllmorchestrator.worker.engine.config.EngineFileConfig;
import com.openllmorchestrator.worker.engine.stage.StagePlan;
import com.openllmorchestrator.worker.engine.stage.resolver.StageResolver;

/**
 * One-time bootstrap state: config, execution hierarchy (plan + resolver), and resolver.
 * Built once at startup and reused for the entire container lifecycle.
 * <p>
 * <b>No transactional data:</b> This runtime holds only config-derived, immutable state.
 * No workflow ID, request context, or per-execution data is stored here to avoid memory leaks.
 * Per-run state is passed as {@link com.openllmorchestrator.worker.engine.contract.ExecutionContext}
 * at execution time and is never retained by the runtime.
 */
public final class EngineRuntime {

    private static volatile EngineFileConfig config;
    private static volatile StagePlan stagePlan;
    private static volatile StageResolver stageResolver;

    /** Set during bootstrap; never null after successful init. */
    public static EngineFileConfig getConfig() {
        EngineFileConfig c = config;
        if (c == null) {
            throw new IllegalStateException("Engine not bootstrapped. Call WorkerBootstrap.initialize() first.");
        }
        return c;
    }

    public static void setConfig(EngineFileConfig config) {
        EngineRuntime.config = config;
    }

    /** Execution hierarchy (plan) built once at bootstrap; reused for container lifecycle. */
    public static StagePlan getStagePlan() {
        StagePlan p = stagePlan;
        if (p == null) {
            throw new IllegalStateException("Engine not bootstrapped. Stage plan not set.");
        }
        return p;
    }

    public static void setStagePlan(StagePlan stagePlan) {
        EngineRuntime.stagePlan = stagePlan;
    }

    /** Resolves predefined stages via config + plugin bucket, custom stages via custom bucket. */
    public static StageResolver getStageResolver() {
        StageResolver r = stageResolver;
        if (r == null) {
            throw new IllegalStateException("Engine not bootstrapped. Stage resolver not set.");
        }
        return r;
    }

    public static void setStageResolver(StageResolver stageResolver) {
        EngineRuntime.stageResolver = stageResolver;
    }

    /** Backward compatibility; set by bootstrap together with config. Prefer getConfig(). */
    @Deprecated
    public static EngineFileConfig CONFIG;
}
