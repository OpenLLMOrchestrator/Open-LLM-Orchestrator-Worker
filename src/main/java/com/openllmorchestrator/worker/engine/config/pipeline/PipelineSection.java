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

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/** Pipeline section of engine config. Each pipeline has one root (one or more stages; each stage has one group). */
@Getter
@Setter
@JsonDeserialize(using = PipelineSectionDeserializer.class)
public class PipelineSection {
    private int defaultTimeoutSeconds;
    /** Default for ASYNC groups: ALL | FIRST_SUCCESS | FIRST_FAILURE | ALL_SETTLED */
    private String defaultAsyncCompletionPolicy = "ALL";
    /** Max depth for nested GROUP recursion (default 5). Exceeding throws at plan build. */
    private int defaultMaxGroupDepth = 5;
    /** Default merge policy hook for ASYNC groups: type MERGE_POLICY, pluginType MergePolicy, name = activity/FQCN. */
    private MergePolicyConfig mergePolicy;
    /** Legacy: single root GROUP/STAGE tree. Used when stages and rootByStage are null/empty. */
    private NodeConfig root;
    /** Optional: stage name → plugin id. When absent, engine-level stagePlugins are used. */
    private Map<String, String> stagePlugins;
    /**
     * Top-level flow: ordered list of stages. Each stage has groups; group children are activity names.
     * When non-null and non-empty, plan is built from this instead of root/rootByStage.
     */
    private List<StageBlockConfig> stages;
    /**
     * Stage name → GROUP config. Execution order follows engine stageOrder; only stages present here are included.
     * When non-null and non-empty, plan is built from this (stagePlugins section not needed).
     */
    private Map<String, NodeConfig> rootByStage;
}

