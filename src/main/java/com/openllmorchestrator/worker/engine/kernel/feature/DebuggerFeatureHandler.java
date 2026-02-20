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
package com.openllmorchestrator.worker.engine.kernel.feature;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.openllmorchestrator.worker.contract.CapabilityResult;
import com.openllmorchestrator.worker.engine.activity.DebugPushActivity;
import com.openllmorchestrator.worker.engine.config.FeatureFlag;
import com.openllmorchestrator.worker.engine.contract.ExecutionContext;
import com.openllmorchestrator.worker.engine.capability.CapabilityPlan;
import com.openllmorchestrator.worker.engine.kernel.interceptor.CapabilityContext;
import com.openllmorchestrator.worker.engine.model.ExecutionModelSerde;
import lombok.extern.slf4j.Slf4j;

/**
 * When DEBUGGER is enabled and context has debugID and debugPushActivity (set by workflow),
 * pushes serialized execution tree and context to Redis before and after every capability (node).
 */
@Slf4j
public final class DebuggerFeatureHandler implements FeatureHandler {

    /** Keys in ExecutionContext.state set by workflow when debug=true, debugID present. */
    public static final String STATE_KEY_DEBUG_ID = "debugID";
    public static final String STATE_KEY_DEBUG_PUSH_ACTIVITY = "debugPushActivity";
    public static final String STATE_KEY_DEBUG_PLAN = "debugPlan";

    @Override
    public FeatureFlag getFeature() {
        return FeatureFlag.DEBUGGER;
    }

    @Override
    public void beforeCapability(CapabilityContext ctx) {
        pushIfDebug(ctx);
    }

    @Override
    public void afterCapability(CapabilityContext ctx, CapabilityResult result) {
        pushIfDebug(ctx);
    }

    @Override
    public void onError(CapabilityContext ctx, Exception e) {
        pushIfDebug(ctx);
    }

    private void pushIfDebug(CapabilityContext ctx) {
        ExecutionContext executionContext = ctx != null ? ctx.getExecutionContext() : null;
        if (executionContext == null) return;

        String debugID = getString(executionContext, STATE_KEY_DEBUG_ID);
        Object stubObj = executionContext.get(STATE_KEY_DEBUG_PUSH_ACTIVITY);
        if (debugID == null || debugID.isEmpty() || !(stubObj instanceof DebugPushActivity)) return;

        try {
            CapabilityPlan plan = getPlan(executionContext);
            String planJson = plan != null ? ExecutionModelSerde.serializePlan(plan) : "null";
            ObjectNode ctxNode = ExecutionModelSerde.objectMapper().createObjectNode();
            ctxNode.set("command", ExecutionModelSerde.objectMapper().valueToTree(executionContext.getCommand()));
            ctxNode.set("versionedState", ExecutionModelSerde.objectMapper().valueToTree(executionContext.getVersionedState()));
            String contextJson = ExecutionModelSerde.objectMapper().writeValueAsString(ctxNode);
            ((DebugPushActivity) stubObj).push(debugID, planJson, contextJson);
        } catch (Exception e) {
            log.warn("Debug push to Redis failed (debugID={})", debugID, e);
        }
    }

    private static String getString(ExecutionContext c, String key) {
        Object v = c.get(key);
        return v != null ? String.valueOf(v).trim() : null;
    }

    @SuppressWarnings("unchecked")
    private static CapabilityPlan getPlan(ExecutionContext c) {
        Object v = c.get(STATE_KEY_DEBUG_PLAN);
        return v instanceof CapabilityPlan ? (CapabilityPlan) v : null;
    }
}
