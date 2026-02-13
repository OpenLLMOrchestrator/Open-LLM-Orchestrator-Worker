package com.openllmorchestrator.worker.engine.config.redis;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Redis section of engine config. */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RedisConfig {
    private String host;
    private int port;
    private String password;

    public static RedisConfig of(String host, int port, String password) {
        return new RedisConfig(host, port, password != null ? password : "");
    }
}
