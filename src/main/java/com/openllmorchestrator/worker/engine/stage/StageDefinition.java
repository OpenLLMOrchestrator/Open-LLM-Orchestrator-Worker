package com.openllmorchestrator.worker.engine.stage;

import lombok.Builder;
import lombok.Getter;

import java.time.Duration;

@Getter
@Builder
public class StageDefinition {

    private String name;
    private String taskQueue;
    private StageExecutionMode executionMode;
    private int group;
    private Duration timeout;
}
