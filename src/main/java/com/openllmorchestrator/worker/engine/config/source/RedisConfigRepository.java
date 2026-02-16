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
package com.openllmorchestrator.worker.engine.config.source;

import com.openllmorchestrator.worker.engine.config.redis.RedisConfig;
import redis.clients.jedis.Jedis;

/**
 * Reads/writes engine config JSON from Redis.
 * Connection from env (RedisConfig) only.
 */
public final class RedisConfigRepository implements ConfigRepository {
    private static final String CONFIG_KEY = "olo:engine:config";

    private final Jedis jedis;

    public RedisConfigRepository(RedisConfig redis) {
        this.jedis = new Jedis(redis.getHost(), redis.getPort());
        if (redis.getPassword() != null && !redis.getPassword().isBlank()) {
            this.jedis.auth(redis.getPassword());
        }
    }

    @Override
    public String get() {
        try {
            return jedis.get(CONFIG_KEY);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void set(String configJson) {
        if (configJson == null) return;
        try {
            jedis.set(CONFIG_KEY, configJson);
        } catch (Exception ignored) {
            // log and continue
        }
    }
}

