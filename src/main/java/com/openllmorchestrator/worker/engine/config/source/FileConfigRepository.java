package com.openllmorchestrator.worker.engine.config.source;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Reads engine config JSON from mounted file. Path from env. Write not supported. */
public final class FileConfigRepository implements ConfigRepository {
    private final String configFilePath;

    public FileConfigRepository(String configFilePath) {
        this.configFilePath = configFilePath != null ? configFilePath : "config/engine-config.json";
    }

    @Override
    public String get() {
        Path path = Paths.get(configFilePath);
        if (!Files.isRegularFile(path)) {
            return null;
        }
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public void set(String configJson) {
        // File is read-only; persistence goes to Redis/DB only
    }
}
