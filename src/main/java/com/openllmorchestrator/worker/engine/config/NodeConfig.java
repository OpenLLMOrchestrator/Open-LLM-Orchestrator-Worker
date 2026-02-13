package com.openllmorchestrator.worker.engine.config;

import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.List;

/**
 * Represents a node in the hierarchical pipeline config.
 * Can be either a GROUP (container with children) or STAGE (leaf execution unit).
 */
@Getter
@Setter
public class NodeConfig {

    /**
     * "GROUP" or "STAGE"
     */
    private String type;

    /**
     * Stage name when type is STAGE
     */
    private String name;

    /**
     * "SYNC" or "ASYNC" - for GROUP: how to execute children.
     * SYNC = sequential, ASYNC = parallel.
     */
    private String executionMode;

    /**
     * Override timeout for this node (STAGE only)
     */
    private Integer timeoutSeconds;

    /**
     * Child nodes when type is GROUP
     */
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
