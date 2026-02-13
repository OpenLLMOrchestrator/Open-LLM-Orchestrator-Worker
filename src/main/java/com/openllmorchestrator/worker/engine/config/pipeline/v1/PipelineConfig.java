package com.openllmorchestrator.worker.engine.config.pipeline.v1;

import com.openllmorchestrator.worker.engine.config.NodeConfig;
import lombok.Data;

@Data
public class PipelineConfig {

    private int defaultTimeoutSeconds;
    private NodeConfig root;
}
