package com.openllmorchestrator.worker.engine.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import redis.clients.jedis.Jedis;

public class RedisConfigRepository {

    private final Jedis jedis;
    private final ObjectMapper mapper =
            new ObjectMapper();

    public RedisConfigRepository() {
        this.jedis = new Jedis("localhost", 6379);
    }

    public QueueConfig find(String queueName) {
        try {
            String key = "queue:config:" + queueName;
            String json = jedis.get(key);
            if (json == null) return null;

            return mapper.readValue(
                    json, QueueConfig.class);

        } catch (Exception e) {
            return null;
        }
    }

    public void save(QueueConfig config) {
        try {
            String key = "queue:config:"
                    + config.getQueueName();
            jedis.set(key,
                    mapper.writeValueAsString(config));
        } catch (Exception ignored) {
        }
    }
}
