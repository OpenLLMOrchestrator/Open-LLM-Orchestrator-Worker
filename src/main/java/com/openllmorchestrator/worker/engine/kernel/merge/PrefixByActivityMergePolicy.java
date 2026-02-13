package com.openllmorchestrator.worker.engine.kernel.merge;

import java.util.Map;

/**
 * ASYNC: prefix each key with the activity/source id to avoid overwrites and data loss.
 * Example: sourceId "MemoryPlugin", key "result" → accumulated key "MemoryPlugin.result".
 */
public final class PrefixByActivityMergePolicy implements OutputMergePolicy {

    public static final String DEFAULT_SEPARATOR = ".";

    private final String separator;

    public PrefixByActivityMergePolicy() {
        this(DEFAULT_SEPARATOR);
    }

    public PrefixByActivityMergePolicy(String separator) {
        this.separator = separator != null && !separator.isEmpty() ? separator : DEFAULT_SEPARATOR;
    }

    @Override
    public void merge(Map<String, Object> accumulated, Map<String, Object> pluginOutput, String sourceId) {
        if (pluginOutput == null) return;
        String prefix = (sourceId != null && !sourceId.isEmpty() ? sourceId + separator : "");
        for (Map.Entry<String, Object> e : pluginOutput.entrySet()) {
            accumulated.put(prefix + e.getKey(), e.getValue());
        }
    }
}
