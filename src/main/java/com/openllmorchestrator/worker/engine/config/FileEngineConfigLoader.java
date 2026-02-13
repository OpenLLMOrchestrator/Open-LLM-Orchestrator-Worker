package com.openllmorchestrator.worker.engine.config;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;

public class FileEngineConfigLoader {

    public static EngineFileConfig load() {

        try {
            ObjectMapper mapper = new ObjectMapper();

            File file =
                    new File("config/engine-config.json");

            if (!file.exists()) {
                throw new IllegalStateException(
                        "engine-config.json not found");
            }

            return mapper.readValue(
                    file,
                    EngineFileConfig.class);

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to load engine config", e);
        }
    }
}
