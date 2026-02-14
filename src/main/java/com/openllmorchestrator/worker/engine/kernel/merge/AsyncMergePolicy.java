package com.openllmorchestrator.worker.engine.kernel.merge;

import java.util.List;
import java.util.Map;

/**
 * Plugin contract for merging ASYNC group outputs into the accumulated map.
 * Implementations can be registered by name in {@link MergePolicyRegistry} and referenced in config.
 */
public interface AsyncMergePolicy {

    /**
     * Merge all ASYNC stage results into the accumulated output map.
     *
     * @param accumulated mutable accumulated output (updated in place)
     * @param results     list of (activityName, result) in completion or config order
     */
    void mergeAll(Map<String, Object> accumulated, List<AsyncOutputMergePolicy.NamedStageResult> results);
}
