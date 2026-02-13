package com.openllmorchestrator.worker.engine.bootstrap;

import com.openllmorchestrator.worker.engine.config.*;

public class WorkerBootstrap {

    public static EngineFileConfig initialize() {

        System.out.println("Loading engine configuration...");

        EngineFileConfig fileConfig =
                FileEngineConfigLoader.load();

        validate(fileConfig);

        System.out.println(
                "Configuration loaded for queue: "
                        + fileConfig.getWorker().getQueueName());

        return fileConfig;
    }

    private static void validate(EngineFileConfig config) {

        if (config.getWorker() == null ||
                config.getWorker().getQueueName() == null) {

            throw new IllegalStateException(
                    "Queue name missing in config file.");
        }
    }
}

