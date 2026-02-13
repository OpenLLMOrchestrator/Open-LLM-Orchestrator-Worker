package com.openllmorchestrator.worker.engine.kernel.merge;

import java.util.Map;

/**
 * ASYNC: first finished job's output is written first; keys from later completers do not overwrite.
 */
public final class FirstWriterWinsMergePolicy implements OutputMergePolicy {

    public static final FirstWriterWinsMergePolicy INSTANCE = new FirstWriterWinsMergePolicy();

    private FirstWriterWinsMergePolicy() {}

    @Override
    public void merge(Map<String, Object> accumulated, Map<String, Object> pluginOutput, String sourceId) {
        if (pluginOutput == null) return;
        for (Map.Entry<String, Object> e : pluginOutput.entrySet()) {
            accumulated.putIfAbsent(e.getKey(), e.getValue());
        }
    }
}
