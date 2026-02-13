package com.openllmorchestrator.worker.engine.config.pipeline;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/** Pipeline section of engine config. */
@Getter
@Setter
public class PipelineSection {
    private int defaultTimeoutSeconds;
    /** Default for ASYNC groups: ALL | FIRST_SUCCESS | FIRST_FAILURE | ALL_SETTLED */
    private String defaultAsyncCompletionPolicy = "ALL";
    /** Legacy: single root GROUP/STAGE tree. Used when stages is null or empty. */
    private NodeConfig root;
    /** Stage name → plugin id for predefined stages (when using root tree). */
    private Map<String, String> stagePlugins;
    /**
     * Top-level flow: ordered list of stages. Each stage has groups; group children are activity names.
     * When non-null and non-empty, plan is built from this instead of root.
     */
    private List<StageBlockConfig> stages;
}
