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
    /** For STAGE: class name to call (fully qualified class name, e.g. com.example.plugin.AccessControlPluginImpl). Required. */
    private String name;
    /** For STAGE: one of the allowed plugin types (e.g. AccessControlPlugin, MemoryPlugin). Required. */
    private String pluginType;
    private String executionMode;
    private Integer timeoutSeconds;
    /** For GROUP ASYNC: override default policy (ALL | FIRST_SUCCESS | FIRST_FAILURE | ALL_SETTLED). */
    private String asyncCompletionPolicy;
    /** For GROUP ASYNC: output key overwrite policy (name from merge policy registry). Legacy; prefer mergePolicy hook. */
    private String asyncOutputMergePolicy;
    /** For GROUP ASYNC: merge policy hook { "type": "MERGE_POLICY", "pluginType": "MergePolicy", "name": "..." }. */
    private MergePolicyConfig mergePolicy;
    /** For GROUP: max recursion depth for nested groups (overrides pipeline defaultMaxGroupDepth). */
    private Integer maxDepth;
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
