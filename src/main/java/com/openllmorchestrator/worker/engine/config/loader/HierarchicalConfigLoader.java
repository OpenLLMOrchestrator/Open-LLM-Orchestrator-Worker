package com.openllmorchestrator.worker.engine.config.loader;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openllmorchestrator.worker.engine.config.EngineFileConfig;
import com.openllmorchestrator.worker.engine.config.env.EnvConfig;
import com.openllmorchestrator.worker.engine.config.source.ConfigRepository;

/**
 * Loads server config in order: Redis → DB → file.
 * If not found anywhere, sleeps (env: CONFIG_RETRY_SLEEP_SECONDS) and retries.
 * When loaded from file, persists to Redis and DB.
 * Returns full EngineFileConfig with connection config (queue, redis, db) from env.
 */
public final class HierarchicalConfigLoader {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private HierarchicalConfigLoader() {}

    /**
     * Load config once at bootstrap. Blocks until config is found.
     * Connection config (worker, redis, database) is always from env.
     */
    public static EngineFileConfig load(EnvConfig env,
                                        ConfigRepository redisRepo,
                                        ConfigRepository dbRepo,
                                        ConfigRepository fileRepo) {
        int sleepSeconds = env.getConfigRetrySleepSeconds() <= 0 ? 30 : env.getConfigRetrySleepSeconds();
        while (true) {
            String json = redisRepo.get();
            if (json != null && !json.isBlank()) {
                return merge(env, parse(json));
            }
            json = dbRepo.get();
            if (json != null && !json.isBlank()) {
                return merge(env, parse(json));
            }
            json = fileRepo.get();
            if (json != null && !json.isBlank()) {
                EngineFileConfig fromFile = parse(json);
                persistToRedisAndDb(redisRepo, dbRepo, json);
                return merge(env, fromFile);
            }
            try {
                Thread.sleep(sleepSeconds * 1000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Config load interrupted", e);
            }
        }
    }

    private static EngineFileConfig parse(String json) {
        try {
            return MAPPER.readValue(json, EngineFileConfig.class);
        } catch (Exception e) {
            throw new RuntimeException("Invalid engine config JSON", e);
        }
    }

    private static EngineFileConfig merge(EnvConfig env, EngineFileConfig fromStorage) {
        return EngineFileConfig.mergeFromEnv(env, fromStorage);
    }

    private static void persistToRedisAndDb(ConfigRepository redisRepo, ConfigRepository dbRepo, String configJson) {
        redisRepo.set(configJson);
        dbRepo.set(configJson);
    }
}
