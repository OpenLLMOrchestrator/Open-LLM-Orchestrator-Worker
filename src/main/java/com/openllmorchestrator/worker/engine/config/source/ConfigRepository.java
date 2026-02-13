package com.openllmorchestrator.worker.engine.config.source;

/**
 * Source for engine config blob (JSON).
 * Implementations: Redis, DB, file.
 */
public interface ConfigRepository {
    /** Returns config JSON or null if not found. */
    String get();

    /** Stores config JSON. */
    void set(String configJson);
}
