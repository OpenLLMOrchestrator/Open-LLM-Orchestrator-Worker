package com.openllmorchestrator.worker.engine.kernel.merge;

import java.util.Map;

/**
 * Policy for merging one plugin's output into the accumulated output map.
 * Used after each SYNC plugin; also as the building block for ASYNC group merges.
 * Extensible: add new implementations for custom merge behaviour.
 */
public interface OutputMergePolicy {

    /**
     * Merge the current plugin's output into the accumulated map.
     *
     * @param accumulated mutable accumulated output (will be updated)
     * @param pluginOutput output from the current plugin (must not be null)
     * @param sourceId     plugin/activity name (for prefix or ordering; may be null)
     */
    void merge(Map<String, Object> accumulated, Map<String, Object> pluginOutput, String sourceId);
}
