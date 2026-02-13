package com.openllmorchestrator.worker.engine.config;

import lombok.Getter;

@Getter
public class EngineFileConfig {

    private WorkerConfig worker;
    private RedisConfig redis;
    private DatabaseConfig database;
    private PipelineConfig pipeline;

    @Getter
    public static class WorkerConfig {
        private String queueName;
        private boolean strictBoot;
    }

    @Getter
    public static class RedisConfig {
        private String host;
        private int port;
        private String password;
    }

    @Getter
    public static class DatabaseConfig {
        private String url;
        private String username;
        private String password;
    }

    @Getter
    public static class PipelineConfig {
        private int defaultTimeoutSeconds;
        private NodeConfig root;
    }
}
