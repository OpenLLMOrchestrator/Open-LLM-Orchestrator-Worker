package com.openllmorchestrator.worker.engine.config.database;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Database section of engine config. */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DatabaseConfig {
    private String url;
    private String username;
    private String password;

    public static DatabaseConfig of(String url, String username, String password) {
        return new DatabaseConfig(url, username, password != null ? password : "");
    }
}
