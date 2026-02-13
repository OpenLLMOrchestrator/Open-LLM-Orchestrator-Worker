package com.openllmorchestrator.worker.engine.config.pipeline;

import com.openllmorchestrator.worker.engine.config.activity.RetryPolicyConfig;
import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.List;

/** A node in the pipeline tree: GROUP or STAGE. */
@Getter
@Setter
public class NodeConfig {
    private String type;
    private String name;
    private String executionMode;
    private Integer timeoutSeconds;
    /** For GROUP ASYNC: override default policy (ALL | FIRST_SUCCESS | FIRST_FAILURE | ALL_SETTLED). */
    private String asyncCompletionPolicy;
    /** For GROUP ASYNC: output key overwrite policy (FIRST_WINS | LAST_WINS | PREFIX_BY_ACTIVITY). */
    private String asyncOutputMergePolicy;
    /** For STAGE: optional activity timeout overrides (seconds). */
    private Integer scheduleToStartSeconds;
    private Integer scheduleToCloseSeconds;
    /** For STAGE: optional retry override. */
    private RetryPolicyConfig retryPolicy;
    private List<NodeConfig> children;

    public List<NodeConfig> getChildren() {
        return children != null ? children : Collections.emptyList();
    }

    public boolean isGroup() {
        return "GROUP".equalsIgnoreCase(type);
    }

    public boolean isStage() {
        return "STAGE".equalsIgnoreCase(type);
    }
}
