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
import lombok.Setter;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Root engine config. One package per section (OCP). Nothing hardcoded in engine. */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class EngineFileConfig {
    private String configVersion;
    private WorkerConfig worker;
    private TemporalConfig temporal;
    private ActivityDefaultsConfig activity;
    private RedisConfig redis;
    private DatabaseConfig database;
    /**
     * Canonical stage order for execution. When using rootByStage, only stages present there are included,
     * in this order. Not-defined stages are skipped. If null/empty, predefined stage order in code is used.
     */
    private List<String> stageOrder;
    /**
     * Stage name → plugin id for predefined stages (e.g. ACCESS → "default").
     * Used when resolving handlers. Pipeline-level stagePlugins override when set.
     */
    private Map<String, String> stagePlugins;
    /**
     * Async merge policy name → implementation. Value is either a built-in name (FIRST_WINS, LAST_WINS, PREFIX_BY_ACTIVITY)
     * or a fully qualified class name implementing AsyncMergePolicy. Registered at bootstrap; referenced in pipeline/group asyncOutputMergePolicy.
     */
    private Map<String, String> mergePolicies;
    /** Named pipelines: user-defined name → pipeline config (e.g. "chat", "document-extraction", "default"). At least one required. */
    private Map<String, PipelineSection> pipelines;

    /** Merge: connection config (queue, redis, db) from env; server config from storage. */
    public static EngineFileConfig mergeFromEnv(EnvConfig env, EngineFileConfig fromStorage) {
        EngineFileConfig merged = new EngineFileConfig();
        merged.configVersion = fromStorage != null ? fromStorage.configVersion : "1.0";
        merged.worker = env.getWorker();
        merged.redis = env.getRedis();
        merged.database = env.getDatabase();
        merged.temporal = fromStorage != null ? fromStorage.temporal : null;
        merged.activity = fromStorage != null ? fromStorage.activity : null;
        merged.pipelines = fromStorage != null ? fromStorage.pipelines : null;
        merged.stageOrder = fromStorage != null ? fromStorage.stageOrder : null;
        merged.stagePlugins = fromStorage != null ? fromStorage.stagePlugins : null;
        merged.mergePolicies = fromStorage != null ? fromStorage.mergePolicies : null;
        return merged;
    }

    /** Effective merge policies from config (may be null/empty). */
    public Map<String, String> getMergePoliciesEffective() {
        return mergePolicies != null ? mergePolicies : Collections.emptyMap();
    }

    /** Effective stage order: config stageOrder if set, else predefined order in code. */
    public List<String> getStageOrderEffective() {
        if (stageOrder != null && !stageOrder.isEmpty()) {
            return stageOrder;
        }
        return com.openllmorchestrator.worker.engine.stage.predefined.PredefinedStages.orderedNames();
    }

    /** Effective stage plugins: engine-level if set. */
    public Map<String, String> getStagePluginsEffective() {
        return stagePlugins != null && !stagePlugins.isEmpty() ? stagePlugins : Collections.emptyMap();
    }

    /** All pipeline names to use at runtime. Requires at least one pipeline in config. */
    public Map<String, PipelineSection> getPipelinesEffective() {
        return pipelines != null ? pipelines : Collections.emptyMap();
    }
}
