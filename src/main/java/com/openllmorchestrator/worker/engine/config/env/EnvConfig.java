package com.openllmorchestrator.worker.engine.config.env;

import com.openllmorchestrator.worker.engine.config.database.DatabaseConfig;
import com.openllmorchestrator.worker.engine.config.redis.RedisConfig;
import com.openllmorchestrator.worker.engine.config.worker.WorkerConfig;
import lombok.Builder;
import lombok.Getter;

/**
 * Connection config from container environment only.
 * Queue name, Redis, and DB are never taken from config file in production.
 */
@Getter
@Builder
public class EnvConfig {
    private final WorkerConfig worker;
    private final RedisConfig redis;
    private final DatabaseConfig database;
    /** Seconds to sleep between config load retries when not found in Redis/DB/file. */
    private final int configRetrySleepSeconds;
    /** Mounted config file path (fallback when Redis and DB have no config). */
    private final String configFilePath;
    /** Max concurrent workflow task pollers (env: MAX_CONCURRENT_WORKFLOW_TASK_POLLERS). */
    private final int maxConcurrentWorkflowTaskPollers;
    /** Max concurrent activity task pollers (env: MAX_CONCURRENT_ACTIVITY_TASK_POLLERS). */
    private final int maxConcurrentActivityTaskPollers;
    /** Temporal server target (env: TEMPORAL_TARGET). Overrides config when set. */
    private final String temporalTarget;
    /** Temporal namespace (env: TEMPORAL_NAMESPACE). Overrides config when set. */
    private final String temporalNamespace;

    public static EnvConfig fromEnvironment() {
        String queueName = getEnv("QUEUE_NAME", "core-task-queue");
        String redisHost = getEnv("REDIS_HOST", "localhost");
        int redisPort = parseInt(getEnv("REDIS_PORT", "6379"), 6379);
        String redisPassword = getEnv("REDIS_PASSWORD", "");
        String dbUrl = getEnv("DB_URL", "jdbc:postgresql://localhost:5432/olo_config");
        String dbUser = getEnv("DB_USERNAME", "postgres");
        String dbPassword = getEnv("DB_PASSWORD", "postgres");
        int retrySleep = parseInt(getEnv("CONFIG_RETRY_SLEEP_SECONDS", "30"), 30);
        String configPath = getEnv("CONFIG_FILE_PATH", getEnv("engine.config.path", "config/engine-config.json"));
        int workflowPollers = parseInt(getEnv("MAX_CONCURRENT_WORKFLOW_TASK_POLLERS", "5"), 5);
        int activityPollers = parseInt(getEnv("MAX_CONCURRENT_ACTIVITY_TASK_POLLERS", "10"), 10);
        String temporalTarget = getEnv("TEMPORAL_TARGET", "localhost:7233");
        String temporalNamespace = getEnv("TEMPORAL_NAMESPACE", "default");

        return EnvConfig.builder()
                .worker(WorkerConfig.of(queueName, false))
                .redis(RedisConfig.of(redisHost, redisPort, redisPassword))
                .database(DatabaseConfig.of(dbUrl, dbUser, dbPassword))
                .configRetrySleepSeconds(retrySleep)
                .configFilePath(configPath)
                .maxConcurrentWorkflowTaskPollers(workflowPollers)
                .maxConcurrentActivityTaskPollers(activityPollers)
                .temporalTarget(temporalTarget)
                .temporalNamespace(temporalNamespace)
                .build();
    }

    private static String getEnv(String key, String defaultValue) {
        String v = System.getenv(key);
        if (v != null && !v.isBlank()) return v.trim();
        return System.getProperty(key, defaultValue);
    }

    private static int parseInt(String s, int defaultValue) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
