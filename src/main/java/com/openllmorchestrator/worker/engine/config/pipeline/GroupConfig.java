package com.openllmorchestrator.worker.engine.config.pipeline;

import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.List;

/**
 * A group within a stage: sync or async execution, with recursive nesting.
 * Children are activity names (plugin ids) or nested GroupConfigs.
 */
@Getter
@Setter
public class GroupConfig {
    /** SYNC or ASYNC */
    private String executionMode;
    /** For ASYNC: ALL | FIRST_SUCCESS | FIRST_FAILURE | ALL_SETTLED */
    private String asyncCompletionPolicy;
    /** For ASYNC: output key overwrite policy (name from merge policy registry). Legacy; prefer mergePolicy hook. */
    private String asyncOutputMergePolicy;
    /** For ASYNC: merge policy hook { "type": "MERGE_POLICY", "pluginType": "MergePolicy", "name": "..." }. */
    private MergePolicyConfig mergePolicy;
    /** Max recursion depth for nested groups (overrides pipeline defaultMaxGroupDepth). */
    private Integer maxDepth;
    private Integer timeoutSeconds;
    /**
     * Children: each element is either a String (activity/plugin name) or a Map (nested group).
     * Use {@link #getChildrenAsList()} and interpret per element.
     */
    private List<Object> children;

    public List<Object> getChildrenAsList() {
        return children != null ? children : Collections.emptyList();
    }

    public boolean isAsync() {
        return "ASYNC".equalsIgnoreCase(executionMode);
    }

    public boolean isSync() {
        return "SYNC".equalsIgnoreCase(executionMode);
    }
}
