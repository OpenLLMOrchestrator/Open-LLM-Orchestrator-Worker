package com.openllmorchestrator.worker.engine.config.pipeline.v1;

import com.openllmorchestrator.worker.engine.config.EngineFileConfig;
import lombok.Data;

@Data
public class EnginePipelineConfig {

    private String configVersion; // must be "1.0"

    private EngineFileConfig.WorkerConfig worker;
    private EngineFileConfig.RedisConfig redis;
    private EngineFileConfig.DatabaseConfig database;

    private PipelineConfig pipeline;
}
