package com.openllmorchestrator.worker.engine.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.openllmorchestrator.worker.engine.config.activity.ActivityDefaultsConfig;
import com.openllmorchestrator.worker.engine.config.database.DatabaseConfig;
import com.openllmorchestrator.worker.engine.config.env.EnvConfig;
import com.openllmorchestrator.worker.engine.config.pipeline.PipelineSection;
import com.openllmorchestrator.worker.engine.config.redis.RedisConfig;
import com.openllmorchestrator.worker.engine.config.temporal.TemporalConfig;
import com.openllmorchestrator.worker.engine.config.worker.WorkerConfig;
import lombok.Getter;

/** Root engine config. One package per section (OCP). Nothing hardcoded in engine. */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class EngineFileConfig {
    private String configVersion;
    private WorkerConfig worker;
    private TemporalConfig temporal;
    private ActivityDefaultsConfig activity;
    private RedisConfig redis;
    private DatabaseConfig database;
    private PipelineSection pipeline;

    /** Merge: connection config (queue, redis, db) from env; server config from storage. */
    public static EngineFileConfig mergeFromEnv(EnvConfig env, EngineFileConfig fromStorage) {
        EngineFileConfig merged = new EngineFileConfig();
        merged.configVersion = fromStorage != null ? fromStorage.configVersion : "1.0";
        merged.worker = env.getWorker();
        merged.redis = env.getRedis();
        merged.database = env.getDatabase();
        merged.temporal = fromStorage != null ? fromStorage.temporal : null;
        merged.activity = fromStorage != null ? fromStorage.activity : null;
        merged.pipeline = fromStorage != null ? fromStorage.pipeline : null;
        return merged;
    }
}
