package com.openllmorchestrator.worker.engine.config.source;

import com.openllmorchestrator.worker.engine.config.database.DatabaseConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Reads/writes engine config JSON from DB. Connection from env only. */
public final class DbConfigRepository implements ConfigRepository {
    private static final String CONFIG_KEY = "engine_config";
    private static final String TABLE = "olo_config";

    private final DatabaseConfig db;

    public DbConfigRepository(DatabaseConfig db) {
        this.db = db;
    }

    @Override
    public String get() {
        try (Connection conn = DriverManager.getConnection(db.getUrl(), db.getUsername(), db.getPassword())) {
            ensureTable(conn);
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT config_value FROM " + TABLE + " WHERE config_key = ?")) {
                ps.setString(1, CONFIG_KEY);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getString(1) : null;
                }
            }
        } catch (SQLException e) {
            return null;
        }
    }

    @Override
    public void set(String configJson) {
        if (configJson == null) return;
        try (Connection conn = DriverManager.getConnection(db.getUrl(), db.getUsername(), db.getPassword())) {
            ensureTable(conn);
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO " + TABLE + " (config_key, config_value) VALUES (?, ?) ON CONFLICT (config_key) DO UPDATE SET config_value = EXCLUDED.config_value")) {
                ps.setString(1, CONFIG_KEY);
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
