package com.openllmorchestrator.worker.engine.bootstrap.steps;

import com.openllmorchestrator.worker.engine.bootstrap.BootstrapContext;
import com.openllmorchestrator.worker.engine.bootstrap.BootstrapStep;
import com.openllmorchestrator.worker.engine.config.loader.HierarchicalConfigLoader;
import com.openllmorchestrator.worker.engine.config.source.ConfigRepository;
import com.openllmorchestrator.worker.engine.config.source.DbConfigRepository;
import com.openllmorchestrator.worker.engine.config.source.FileConfigRepository;
import com.openllmorchestrator.worker.engine.config.source.RedisConfigRepository;

/**
 * Load config: Redis → DB → file. Sleep and retry until found.
 * If from file, persist to Redis and DB. Queue/Redis/DB from env only.
 */
public final class LoadConfigStep implements BootstrapStep {
    @Override
    public void run(BootstrapContext ctx) {
        var env = ctx.getEnvConfig();
        if (env == null) {
            env = com.openllmorchestrator.worker.engine.config.env.EnvConfig.fromEnvironment();
            ctx.setEnvConfig(env);
        }
        ConfigRepository redisRepo = new RedisConfigRepository(env.getRedis());
        ConfigRepository dbRepo = new DbConfigRepository(env.getDatabase());
        ConfigRepository fileRepo = new FileConfigRepository(env.getConfigFilePath());
        ctx.setConfig(HierarchicalConfigLoader.load(env, redisRepo, dbRepo, fileRepo));
    }
}
