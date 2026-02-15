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
