/*
 * Copyright 2026 Open LLM Orchestrator contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.openllmorchestrator.worker.engine.contract;

import lombok.Builder;
import lombok.Getter;

/**
 * Node in an execution graph (future DAG support). Identified by id; stageBucketName is the stage/bucket to run.
 */
@Getter
@Builder
public class StageNode {
    private final String id;
    private final String stageBucketName;

    public static StageNode of(String stageBucketName) {
        return StageNode.builder().id(stageBucketName).stageBucketName(stageBucketName).build();
    }
}

