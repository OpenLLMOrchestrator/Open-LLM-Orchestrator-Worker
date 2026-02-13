package com.openllmorchestrator.worker.engine.kernel.merge;

import com.openllmorchestrator.worker.engine.contract.StageResult;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Policy for merging multiple ASYNC plugin outputs into the accumulated map.
 * Order of results may be completion order (first finished first, or last).
 * Extensible: register custom strategies by name.
 */
public enum AsyncOutputMergePolicy {
    /** First finished job's output written first; later outputs do not overwrite keys (putIfAbsent). */
    FIRST_WINS(FirstWriterWinsMergePolicy.INSTANCE),
    /** Last finished overwrites; merge in completion order (putAll each). */
    LAST_WINS(LastWriterWinsMergePolicy.INSTANCE),
    /** Each plugin's keys prefixed by activity name to avoid data loss. */
    PREFIX_BY_ACTIVITY(new PrefixByActivityMergePolicy());

    private final OutputMergePolicy delegate;

    AsyncOutputMergePolicy(OutputMergePolicy delegate) {
        this.delegate = delegate;
    }

    /**
     * Merge a list of (activityName, result) into the accumulated map.
     * For FIRST_WINS: merge in list order (first in list = first writer).
     * For LAST_WINS: merge in list order (last overwrites).
     * For PREFIX_BY_ACTIVITY: each result's keys are prefixed by activity name.
     */
    public void mergeAll(Map<String, Object> accumulated, List<NamedStageResult> results) {
        if (results == null || results.isEmpty()) return;
        for (NamedStageResult r : results) {
            Map<String, Object> data = r.getResult() != null && r.getResult().getData() != null
                    ? r.getResult().getData()
                    : Collections.emptyMap();
            delegate.merge(accumulated, data, r.getActivityName());
        }
    }

    public static AsyncOutputMergePolicy fromConfig(String value) {
        if (value == null || value.isBlank()) return LAST_WINS;
        switch (value.toUpperCase()) {
            case "FIRST_WINS":
            case "FIRST_FINISHED_FIRST":
                return FIRST_WINS;
            case "LAST_WINS":
            case "LAST_FINISHED_WINS":
            case "FIRST_FINISHED_LAST":
                return LAST_WINS;
            case "PREFIX_BY_ACTIVITY":
            case "PREFIX":
                return PREFIX_BY_ACTIVITY;
            default:
                return LAST_WINS;
        }
    }

    /** Pair of activity name and its stage result (for ordered merge). */
    @Getter
    public static class NamedStageResult {
        private final String activityName;
        private final StageResult result;

        public NamedStageResult(String activityName, StageResult result) {
            this.activityName = activityName;
            this.result = result;
        }

        public static List<NamedStageResult> from(List<String> names, List<StageResult> results) {
            if (names == null || results == null || names.size() != results.size()) {
                return Collections.emptyList();
            }
            List<NamedStageResult> out = new ArrayList<>(names.size());
            for (int i = 0; i < names.size(); i++) {
                out.add(new NamedStageResult(names.get(i), results.get(i)));
            }
            return out;
        }
    }
}
