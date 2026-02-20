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
package com.openllmorchestrator.worker.engine.kernel.interceptor;

import com.openllmorchestrator.worker.engine.capability.CapabilityGroupSpec;
import com.openllmorchestrator.worker.engine.contract.ExecutionContext;
import com.openllmorchestrator.worker.contract.CapabilityResult;

/**
 * Kernel-level interceptor. Invoked before/after each capability, each group, and each branch (expression) in conditionals.
 */
public interface ExecutionInterceptor {

    void beforeCapability(CapabilityContext ctx);

    void afterCapability(CapabilityContext ctx, CapabilityResult result);

    void onError(CapabilityContext ctx, Exception e);

    /** Before executing a group (sync/async/conditional). Default no-op. */
    default void beforeGroup(ExecutionContext context, int groupIndex, CapabilityGroupSpec spec) {}

    /** After a group completes. Default no-op. */
    default void afterGroup(ExecutionContext context, int groupIndex, CapabilityGroupSpec spec) {}

    /** Before executing the selected branch of a conditional (expression node). Default no-op. */
    default void beforeBranch(ExecutionContext context, int groupIndex, int branchIndex) {}

    /** After the selected branch completes. Default no-op. */
    default void afterBranch(ExecutionContext context, int groupIndex, int branchIndex) {}

    /** Before entering a capability node (config-level bucket, e.g. ACCESS, MODEL). Default no-op. */
    default void beforeCapabilityNode(ExecutionContext context, String capabilityName, String capabilityNodeId) {}

    /** After exiting a capability node. Default no-op. */
    default void afterCapabilityNode(ExecutionContext context, String capabilityName, String capabilityNodeId) {}
}

