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
    /** For ASYNC: output key overwrite policy (FIRST_WINS | LAST_WINS | PREFIX_BY_ACTIVITY). */
    private String asyncOutputMergePolicy;
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
