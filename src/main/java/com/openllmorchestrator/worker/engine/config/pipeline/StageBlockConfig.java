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
package com.openllmorchestrator.worker.engine.config.pipeline;

import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.List;

/**
 * Top-level stage in the pipeline flow. Each stage has one or more groups (sync/async, recursive).
 * Group children are activity names (plugin ids), each implemented by one StageHandler.
 */
@Getter
@Setter
public class StageBlockConfig {
    /** Stage name (e.g. ACCESS, MEMORY, MODEL). */
    private String stage;
    /** Groups within this stage; order preserved. */
    private List<GroupConfig> groups;

    public List<GroupConfig> getGroupsSafe() {
        return groups != null ? groups : Collections.emptyList();
    }
}
