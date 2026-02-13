package com.openllmorchestrator.worker.engine.config;

import lombok.Builder;
import lombok.Getter;

import java.time.Duration;
import java.util.List;

@Getter
@Builder
public class QueueConfig {

    private String queueName;
    private List<String> stages;
    private Duration defaultTimeout;
}
