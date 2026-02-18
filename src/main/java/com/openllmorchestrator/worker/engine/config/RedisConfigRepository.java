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
package com.openllmorchestrator.worker.engine.config;

import com.openllmorchestrator.worker.engine.config.EngineConfigMapper;
import redis.clients.jedis.Jedis;

public class RedisConfigRepository {

    private static final EngineConfigMapper MAPPER = EngineConfigMapper.getInstance();

    private final Jedis jedis;

    public RedisConfigRepository() {
        this.jedis = new Jedis("localhost", 6379);
    }

    public QueueConfig find(String queueName) {
        try {
            String key = "queue:config:" + queueName;
            String json = jedis.get(key);
            if (json == null) return null;

            return MAPPER.queueConfigFromJson(json);

        } catch (Exception e) {
            return null;
        }
    }

    public void save(QueueConfig config) {
        try {
            String key = "queue:config:"
                    + config.getQueueName();
            jedis.set(key, MAPPER.toJson(config));
        } catch (Exception ignored) {
        }
    }
}

