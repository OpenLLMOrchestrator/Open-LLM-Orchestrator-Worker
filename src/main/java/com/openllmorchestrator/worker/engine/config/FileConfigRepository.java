package com.openllmorchestrator.worker.engine.config;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;

public class FileConfigRepository {

    private final ObjectMapper mapper =
            new ObjectMapper();

    public QueueConfig find(String queueName) {

        try {
            File file =
                    new File("config/" +
                            queueName + ".json");

            if (!file.exists())
                return null;

            return mapper.readValue(
                    file,
                    QueueConfig.class);

        } catch (Exception e) {
            return null;
        }
    }
}
