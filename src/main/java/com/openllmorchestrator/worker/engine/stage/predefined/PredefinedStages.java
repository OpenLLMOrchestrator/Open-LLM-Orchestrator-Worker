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
    public static final String PRE_CONTEXT_SETUP = "PRE_CONTEXT_SETUP";
    /** LLM call to design further execution structure; typically runs last in planning phase. */
    public static final String PLANNER = "PLANNER";
    public static final String PLAN_EXECUTOR = "PLAN_EXECUTOR";
    public static final String EXECUTION_CONTROLLER = "EXECUTION_CONTROLLER";
    public static final String ITERATIVE_BLOCK = "ITERATIVE_BLOCK";
    public static final String MODEL = "MODEL";
    public static final String RETRIEVAL = "RETRIEVAL";
    public static final String TOOL = "TOOL";
    public static final String MCP = "MCP";
    public static final String MEMORY = "MEMORY";
    public static final String REFLECTION = "REFLECTION";
    public static final String SUB_OBSERVABILITY = "SUB_OBSERVABILITY";
    public static final String SUB_CUSTOM = "SUB_CUSTOM";
    public static final String ITERATIVE_BLOCK_END = "ITERATIVE_BLOCK_END";
    public static final String FILTER = "FILTER";
    public static final String CUSTOM = "CUSTOM";
    public static final String POST_PROCESS = "POST_PROCESS";
    public static final String OBSERVABILITY = "OBSERVABILITY";

    private static final Set<String> NAMES = Set.of(
            ACCESS, PRE_CONTEXT_SETUP, PLANNER, PLAN_EXECUTOR, EXECUTION_CONTROLLER, ITERATIVE_BLOCK,
            MODEL, RETRIEVAL, TOOL, MCP, MEMORY, REFLECTION, SUB_OBSERVABILITY,
            SUB_CUSTOM, ITERATIVE_BLOCK_END, FILTER, POST_PROCESS, OBSERVABILITY, CUSTOM);

    /** Canonical order for execution when config does not set stageOrder. */
    private static final List<String> ORDERED_NAMES = List.of(
            ACCESS, PRE_CONTEXT_SETUP, PLANNER, PLAN_EXECUTOR, EXECUTION_CONTROLLER, ITERATIVE_BLOCK,
            MODEL, RETRIEVAL, TOOL, MCP, MEMORY, REFLECTION, SUB_OBSERVABILITY,
            SUB_CUSTOM, ITERATIVE_BLOCK_END, FILTER, POST_PROCESS, OBSERVABILITY, CUSTOM);

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

