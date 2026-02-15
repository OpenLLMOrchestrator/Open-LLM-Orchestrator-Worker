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
package com.openllmorchestrator.worker.engine.stage.predefined;

import java.util.List;
import java.util.Set;

/** Predefined pipeline stage names. Others are custom and use custom bucket. */
public final class PredefinedStages {
    public static final String ACCESS = "ACCESS";
    public static final String MEMORY = "MEMORY";
    public static final String RETRIEVAL = "RETRIEVAL";
    public static final String MODEL = "MODEL";
    public static final String MCP = "MCP";
    public static final String TOOL = "TOOL";
    public static final String FILTER = "FILTER";
    public static final String POST_PROCESS = "POST_PROCESS";
    public static final String OBSERVABILITY = "OBSERVABILITY";
    public static final String CUSTOM = "CUSTOM";

    private static final Set<String> NAMES = Set.of(
            ACCESS, MEMORY, RETRIEVAL, MODEL, MCP, TOOL, FILTER, POST_PROCESS, OBSERVABILITY, CUSTOM);

    /** Canonical order for execution when config does not set stageOrder. */
    private static final List<String> ORDERED_NAMES = List.of(
            ACCESS, MEMORY, RETRIEVAL, MODEL, MCP, TOOL, FILTER, POST_PROCESS, OBSERVABILITY, CUSTOM);

    private PredefinedStages() {}

    public static boolean isPredefined(String stageName) {
        return stageName != null && NAMES.contains(stageName);
    }

    public static Set<String> all() {
        return NAMES;
    }

    /** Ordered list of predefined stage names for plan building when stageOrder is not in config. */
    public static List<String> orderedNames() {
        return ORDERED_NAMES;
    }
}
