package com.openllmorchestrator.worker.engine.config.temporal;

import lombok.Getter;

/** Temporal server connection. No hardcoded targets in engine. */
@Getter
public class TemporalConfig {
    private String target = "localhost:7233";
    private String namespace = "default";
}
