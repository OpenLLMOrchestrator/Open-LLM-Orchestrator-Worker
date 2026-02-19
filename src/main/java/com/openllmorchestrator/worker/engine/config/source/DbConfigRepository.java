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

import com.openllmorchestrator.worker.engine.config.database.DatabaseConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Reads/writes engine config JSON from DB. Connection from env only. Per-queue: config_key = engine_config:queueName. */
public final class DbConfigRepository implements ConfigRepository {
    private static final String CONFIG_KEY_DEFAULT = "engine_config";
    private static final String TABLE = "olo_config";

    private final DatabaseConfig db;
    private final String configKey;

    public DbConfigRepository(DatabaseConfig db) {
        this(db, null);
    }

    /** @param queueName when set, use config_key = engine_config:queueName. Null → engine_config. */
    public DbConfigRepository(DatabaseConfig db, String queueName) {
        this.db = db;
        this.configKey = queueName != null && !queueName.isBlank()
                ? CONFIG_KEY_DEFAULT + ":" + queueName.trim()
                : CONFIG_KEY_DEFAULT;
    }

    @Override
    public String get() {
        try (Connection conn = DriverManager.getConnection(db.getUrl(), db.getUsername(), db.getPassword())) {
            ensureTable(conn);
            String value = getByKey(conn, configKey);
            if (value != null && !value.isBlank()) {
                return value;
            }
            if (!CONFIG_KEY_DEFAULT.equals(configKey)) {
                return getByKey(conn, CONFIG_KEY_DEFAULT);
            }
            return value;
        } catch (SQLException e) {
            return null;
        }
    }

    private static String getByKey(Connection conn, String key) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT config_value FROM " + TABLE + " WHERE config_key = ?")) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    @Override
    public void set(String configJson) {
        if (configJson == null) return;
        try (Connection conn = DriverManager.getConnection(db.getUrl(), db.getUsername(), db.getPassword())) {
            ensureTable(conn);
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO " + TABLE + " (config_key, config_value) VALUES (?, ?) ON CONFLICT (config_key) DO UPDATE SET config_value = EXCLUDED.config_value")) {
                ps.setString(1, configKey);
                ps.setString(2, configJson);
                ps.executeUpdate();
            }
        } catch (SQLException ignored) {
        }
    }

    private static void ensureTable(Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "CREATE TABLE IF NOT EXISTS " + TABLE + " (config_key VARCHAR(255) PRIMARY KEY, config_value TEXT)")) {
            ps.execute();
        }
    }
}

