package com.openllmorchestrator.worker.engine.config.pipeline;

import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.List;

/**
 * Top-level stage in the pipeline flow. Each stage has one or more groups (sync/async, recursive).
 * Group children are activity names (plugin ids), each implemented by one StageHandler.
 */
@Getter
@Setter
public class StageBlockConfig {
    /** Stage name (e.g. ACCESS, MEMORY, MODEL). */
    private String stage;
    /** Groups within this stage; order preserved. */
    private List<GroupConfig> groups;

    public List<GroupConfig> getGroupsSafe() {
        return groups != null ? groups : Collections.emptyList();
    }
}
