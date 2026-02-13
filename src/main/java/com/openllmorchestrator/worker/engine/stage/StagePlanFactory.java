package com.openllmorchestrator.worker.engine.stage;

import com.openllmorchestrator.worker.engine.config.EngineFileConfig;
import com.openllmorchestrator.worker.engine.config.NodeConfig;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class StagePlanFactory {

    public static StagePlan fromFileConfig(EngineFileConfig fileConfig) {

        NodeConfig root = fileConfig.getPipeline().getRoot();
        if (root == null) {
            throw new IllegalStateException("Pipeline root is missing in config.");
        }

        int defaultTimeout = fileConfig.getPipeline().getDefaultTimeoutSeconds();
        String taskQueue = fileConfig.getWorker().getQueueName();

        StagePlan.Builder builder = StagePlan.builder();
        processNode(root, defaultTimeout, taskQueue, builder);
        return builder.build();
    }

    /**
     * Recursively process a pipeline node and append groups to the builder.
     * - STAGE: adds a single sync group with that stage
     * - GROUP SYNC: processes each child sequentially (each child adds its groups)
     * - GROUP ASYNC: collects all leaf stages and adds one parallel group
     */
    private static void processNode(
            NodeConfig node,
            int defaultTimeout,
            String taskQueue,
            StagePlan.Builder builder) {

        if (node.isStage()) {
            int timeout = node.getTimeoutSeconds() != null
                    ? node.getTimeoutSeconds()
                    : defaultTimeout;
            builder.addSyncWithCustomConfig(
                    node.getName(),
                    StageExecutionMode.SYNC,
                    Duration.ofSeconds(timeout),
                    taskQueue
            );
            return;
        }

        if (node.isGroup()) {
            boolean isAsync = "ASYNC".equalsIgnoreCase(node.getExecutionMode());

            if (isAsync) {
                List<String> stageNames = collectStageNames(node);
                int timeout = node.getTimeoutSeconds() != null
                        ? node.getTimeoutSeconds()
                        : defaultTimeout;
                builder.addAsyncGroup(
                        stageNames,
                        Duration.ofSeconds(timeout),
                        taskQueue
                );
            } else {
                for (NodeConfig child : node.getChildren()) {
                    processNode(child, defaultTimeout, taskQueue, builder);
                }
            }
        }
    }

    /**
     * Collect all stage names from a GROUP (flattens nested groups).
     * Used for ASYNC groups where all children run in parallel.
     */
    private static List<String> collectStageNames(NodeConfig node) {
        List<String> names = new ArrayList<>();
        for (NodeConfig child : node.getChildren()) {
            if (child.isStage()) {
                names.add(child.getName());
            } else if (child.isGroup()) {
                names.addAll(collectStageNames(child));
            }
        }
        return names;
    }
}
