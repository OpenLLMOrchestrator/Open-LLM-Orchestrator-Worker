package com.openllmorchestrator.worker.engine.config.worker;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Worker section of engine config. */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class WorkerConfig {
    private String queueName;
    private boolean strictBoot;

    public static WorkerConfig of(String queueName, boolean strictBoot) {
        return new WorkerConfig(queueName, strictBoot);
    }
}
