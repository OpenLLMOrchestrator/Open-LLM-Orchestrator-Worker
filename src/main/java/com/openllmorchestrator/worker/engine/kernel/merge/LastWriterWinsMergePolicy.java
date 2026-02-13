package com.openllmorchestrator.worker.engine.kernel.merge;

import java.util.Map;

/**
 * ASYNC: last finished job's output overwrites; same as putAll.
 */
public final class LastWriterWinsMergePolicy implements OutputMergePolicy {

    public static final LastWriterWinsMergePolicy INSTANCE = new LastWriterWinsMergePolicy();

    private LastWriterWinsMergePolicy() {}

    @Override
    public void merge(Map<String, Object> accumulated, Map<String, Object> pluginOutput, String sourceId) {
        if (pluginOutput != null && !pluginOutput.isEmpty()) {
            accumulated.putAll(pluginOutput);
        }
    }
}
