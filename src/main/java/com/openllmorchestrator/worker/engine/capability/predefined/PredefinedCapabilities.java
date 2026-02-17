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
package com.openllmorchestrator.worker.engine.capability.predefined;

import java.util.List;
import java.util.Set;

/**
 * Predefined capability names (fixed flow). Users get a fixed order for these; they can also define
 * custom capabilities in config (capabilities map) and reference any capability—predefined or custom—anywhere in the flow.
 */
public final class PredefinedCapabilities {
    public static final String ACCESS = "ACCESS";
    public static final String PRE_CONTEXT_SETUP = "PRE_CONTEXT_SETUP";
    /** LLM call to design further execution structure; typically runs last in planning phase. */
    public static final String PLANNER = "PLANNER";
    public static final String PLAN_EXECUTOR = "PLAN_EXECUTOR";
    public static final String EXECUTION_CONTROLLER = "EXECUTION_CONTROLLER";
    public static final String ITERATIVE_BLOCK = "ITERATIVE_BLOCK";
    public static final String MODEL = "MODEL";
    public static final String RETRIEVAL = "RETRIEVAL";
    /** Alias for RETRIEVAL; same semantics (retrieve from vector store / search). */
    public static final String RETRIEVE = "RETRIEVE";
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
    /** Score/measure model output (quality, relevance); feeds into learning. Future-ready for evaluation pipelines. */
    public static final String EVALUATION = "EVALUATION";
    /** Alias for evaluation stage (score/measure model output). */
    public static final String EVALUATE = "EVALUATE";
    /** Collect user feedback (ratings, corrections) as training signal. Future-ready for incremental learning. */
    public static final String FEEDBACK = "FEEDBACK";
    /** Alias for feedback capture (user ratings, corrections). */
    public static final String FEEDBACK_CAPTURE = "FEEDBACK_CAPTURE";
    /** Incremental learning: fine-tune, update embeddings, or train on new data. Future-ready for model learning. */
    public static final String LEARNING = "LEARNING";
    /** Build or curate dataset from feedback/evaluations for training. Future-ready for incremental learning. */
    public static final String DATASET_BUILD = "DATASET_BUILD";
    /** Trigger training job (e.g. fine-tune, LoRA) when conditions are met. Future-ready for model learning. */
    public static final String TRAIN_TRIGGER = "TRAIN_TRIGGER";
    /** Register or promote a trained model for serving. Future-ready for model lifecycle. */
    public static final String MODEL_REGISTRY = "MODEL_REGISTRY";
    public static final String OBSERVABILITY = "OBSERVABILITY";

    private static final Set<String> NAMES = Set.of(
            ACCESS, PRE_CONTEXT_SETUP, PLANNER, PLAN_EXECUTOR, EXECUTION_CONTROLLER, ITERATIVE_BLOCK,
            MODEL, RETRIEVAL, RETRIEVE, TOOL, MCP, MEMORY, REFLECTION, SUB_OBSERVABILITY,
            SUB_CUSTOM, ITERATIVE_BLOCK_END, FILTER, POST_PROCESS,
            EVALUATION, EVALUATE, FEEDBACK, FEEDBACK_CAPTURE, LEARNING, DATASET_BUILD, TRAIN_TRIGGER, MODEL_REGISTRY,
            OBSERVABILITY, CUSTOM);

    /** Canonical order for execution when config does not set stageOrder. Learning/training stages (EVALUATE, FEEDBACK_CAPTURE, DATASET_BUILD, TRAIN_TRIGGER, MODEL_REGISTRY) are before OBSERVABILITY. */
    private static final List<String> ORDERED_NAMES = List.of(
            ACCESS, PRE_CONTEXT_SETUP, PLANNER, PLAN_EXECUTOR, EXECUTION_CONTROLLER, ITERATIVE_BLOCK,
            MODEL, RETRIEVAL, RETRIEVE, TOOL, MCP, MEMORY, REFLECTION, SUB_OBSERVABILITY,
            SUB_CUSTOM, ITERATIVE_BLOCK_END, FILTER, POST_PROCESS,
            EVALUATION, EVALUATE, FEEDBACK, FEEDBACK_CAPTURE, LEARNING, DATASET_BUILD, TRAIN_TRIGGER, MODEL_REGISTRY,
            OBSERVABILITY, CUSTOM);

    private PredefinedCapabilities() {}

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

