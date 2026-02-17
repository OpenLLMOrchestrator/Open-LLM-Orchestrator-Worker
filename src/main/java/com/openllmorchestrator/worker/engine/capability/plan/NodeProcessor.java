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
package com.openllmorchestrator.worker.engine.capability.plan;

import com.openllmorchestrator.worker.engine.config.pipeline.NodeConfig;
import com.openllmorchestrator.worker.engine.capability.CapabilityPlanBuilder;

/** Processes one node type. Add new node types by adding implementations (OCP). */
public interface NodeProcessor {
    boolean supports(NodeConfig node);

    void process(NodeConfig node, PlanBuildContext ctx, CapabilityPlanBuilder builder, PipelineWalker walker, int depth);
}

