package com.openllmorchestrator.worker.engine.stage;

import lombok.Getter;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
public class StagePlan {

    private final List<List<StageDefinition>> groups;

    private StagePlan(List<List<StageDefinition>> groups) {
        this.groups = groups;
    }

    public static Builder builder() {
        return new Builder();
    }

    // -------------------------------------
    // Builder
    // -------------------------------------
    public static class Builder {

        private final List<List<StageDefinition>> groups =
                new ArrayList<>();

        private int groupCounter = 0;

        public Builder addSync(String stageName) {

            StageDefinition definition =
                    StageDefinition.builder()
                            .name(stageName)
                            .executionMode(StageExecutionMode.SYNC)
                            .group(groupCounter++)
                            .taskQueue("core-task-queue")
                            .timeout(Duration.ofSeconds(30))
                            .build();

            groups.add(Collections.singletonList(definition));
            return this;
        }

        public Builder addSyncWithCustomConfig(
                String stageName,
                StageExecutionMode mode,
                Duration timeout,
                String taskQueue) {

            StageDefinition definition =
                    StageDefinition.builder()
                            .name(stageName)
                            .executionMode(mode)
                            .group(groupCounter++)
                            .taskQueue(taskQueue)
                            .timeout(timeout)
                            .build();

            groups.add(Collections.singletonList(definition));
            return this;
        }

        /**
         * Adds a group with multiple stages (for ASYNC parallel execution).
         */
        public Builder addAsyncGroup(
                List<String> stageNames,
                Duration timeout,
                String taskQueue) {

            List<StageDefinition> definitions = new ArrayList<>();
            for (String stageName : stageNames) {
                definitions.add(
                        StageDefinition.builder()
                                .name(stageName)
                                .executionMode(StageExecutionMode.ASYNC)
                                .group(groupCounter)
                                .taskQueue(taskQueue)
                                .timeout(timeout)
                                .build()
                );
            }
            groupCounter++;
            groups.add(definitions);
            return this;
        }

        public StagePlan build() {
            return new StagePlan(groups);
        }
    }
}
