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

import com.fasterxml.jackson.databind.JsonNode;
import com.openllmorchestrator.worker.engine.config.EngineConfigMapper;
import com.openllmorchestrator.worker.engine.config.redis.RedisConfig;
import redis.clients.jedis.Jedis;

/**
 * Reads/writes engine config JSON from Redis.
 * Key format: olo:engine:config:&lt;config_key&gt;:&lt;version&gt; (e.g. olo:engine:config:default:1.0).
 * config_key from env CONFIG_KEY (default "default"). Version from env CONFIG_VERSION (default "1.0") on read;
 * on write, version from config JSON configVersion field (default "1.0").
 */
public final class RedisConfigRepository implements ConfigRepository {
    private static final String KEY_PREFIX = "olo:engine:config:";
    private static final String DEFAULT_CONFIG_KEY = "default";
    private static final String DEFAULT_VERSION = "1.0";
    private static final EngineConfigMapper MAPPER = EngineConfigMapper.getInstance();

    private final Jedis jedis;
    private final String configKey;

    public RedisConfigRepository(RedisConfig redis) {
        this(redis, null);
    }

    /** @param configKey queue or config key (e.g. queue name). Null → use env CONFIG_KEY or "default". */
    public RedisConfigRepository(RedisConfig redis, String configKey) {
        this.jedis = new Jedis(redis.getHost(), redis.getPort());
        if (redis.getPassword() != null && !redis.getPassword().isBlank()) {
            this.jedis.auth(redis.getPassword());
        }
        this.configKey = configKey != null && !configKey.isBlank() ? configKey.trim() : null;
    }

    private String getConfigKey() {
        if (configKey != null) return configKey;
        String k = System.getenv("CONFIG_KEY");
        return (k != null && !k.isBlank()) ? k.trim() : DEFAULT_CONFIG_KEY;
    }

    private static String getConfigVersionForRead() {
        String v = System.getenv("CONFIG_VERSION");
        return (v != null && !v.isBlank()) ? v.trim() : DEFAULT_VERSION;
    }

    private static String buildKey(String configKey, String version) {
        return KEY_PREFIX + configKey + ":" + version;
    }

    @Override
    public String get() {
        try {
            String key = buildKey(getConfigKey(), getConfigVersionForRead());
            String value = jedis.get(key);
            if (value != null && !value.isBlank()) {
                return value;
            }
            if (configKey != null && !configKey.equals(DEFAULT_CONFIG_KEY)) {
                String defaultKey = buildKey(DEFAULT_CONFIG_KEY, getConfigVersionForRead());
                return jedis.get(defaultKey);
            }
            return value;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void set(String configJson) {
        if (configJson == null) return;
        try {
            String version = DEFAULT_VERSION;
            JsonNode root = MAPPER.readTree(configJson);
            if (root != null && root.has("configVersion") && root.get("configVersion").isTextual()) {
                String v = root.get("configVersion").asText();
                if (v != null && !v.isBlank()) version = v;
            }
            String key = buildKey(getConfigKey(), version);
            jedis.set(key, configJson);
        } catch (Exception ignored) {
            // log and continue
        }
    }
}

