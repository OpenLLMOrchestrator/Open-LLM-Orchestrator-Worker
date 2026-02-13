package com.openllmorchestrator.worker.engine.kernel.merge;

import java.util.Map;

/** SYNC default: merge current plugin output into accumulated by putAll (later keys overwrite). */
public final class PutAllMergePolicy implements OutputMergePolicy {

    public static final PutAllMergePolicy INSTANCE = new PutAllMergePolicy();

    private PutAllMergePolicy() {}

    @Override
    public void merge(Map<String, Object> accumulated, Map<String, Object> pluginOutput, String sourceId) {
        if (pluginOutput != null && !pluginOutput.isEmpty()) {
            accumulated.putAll(pluginOutput);
        }
    }
}
