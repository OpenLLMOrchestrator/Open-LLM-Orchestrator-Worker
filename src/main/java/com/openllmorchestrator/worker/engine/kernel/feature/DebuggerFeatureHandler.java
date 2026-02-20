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
import com.openllmorchestrator.worker.engine.capability.CapabilityGroupSpec;
import com.openllmorchestrator.worker.engine.capability.CapabilityPlan;
import com.openllmorchestrator.worker.engine.config.FeatureFlag;
import com.openllmorchestrator.worker.engine.contract.ExecutionContext;
import com.openllmorchestrator.worker.engine.kernel.interceptor.CapabilityContext;
import com.openllmorchestrator.worker.engine.model.ExecutionModelSerde;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * When DEBUGGER is enabled, pushes serialized execution tree and context to Redis for every node type:
 * group node, plugin (capability) node, condition node, expression (branch) node. Each has a unique executionNodeId (UUID).
 */
@Slf4j
public final class DebuggerFeatureHandler implements FeatureHandler {

    /** Keys in ExecutionContext.state set by workflow when command.debug=true and command.debugID present. */
    public static final String STATE_KEY_DEBUG_ID = "debugID";
    public static final String STATE_KEY_DEBUG_PUSH_ACTIVITY = "debugPushActivity";
    public static final String STATE_KEY_DEBUG_PLAN = "debugPlan";
    /** Set by ConditionalGroupExecutor so condition capability gets node kind "condition". */
    public static final String STATE_KEY_DEBUG_NODE_KIND = "debugNodeKind";

    private static final String NODE_KIND_GROUP = "group";
    private static final String NODE_KIND_PLUGIN = "plugin";
    private static final String NODE_KIND_CONDITION = "condition";
    private static final String NODE_KIND_EXPRESSION = "expression";

    @Override
    public FeatureFlag getFeature() {
        return FeatureFlag.DEBUGGER;
    }

    @Override
    public void beforeCapability(CapabilityContext ctx) {
        pushIfDebug(ctx, "before");
    }

    @Override
    public void afterCapability(CapabilityContext ctx, CapabilityResult result) {
        pushIfDebug(ctx, "after");
    }

    @Override
    public void onError(CapabilityContext ctx, Exception e) {
        pushIfDebug(ctx, "onError");
    }

    @Override
    public void beforeGroup(ExecutionContext context, int groupIndex, CapabilityGroupSpec spec) {
        pushIfDebugForGroup(context, groupIndex, spec, "before");
    }

    @Override
    public void afterGroup(ExecutionContext context, int groupIndex, CapabilityGroupSpec spec) {
        pushIfDebugForGroup(context, groupIndex, spec, "after");
    }

    @Override
    public void beforeBranch(ExecutionContext context, int groupIndex, int branchIndex) {
        pushIfDebugForBranch(context, groupIndex, branchIndex, "before");
    }

    @Override
    public void afterBranch(ExecutionContext context, int groupIndex, int branchIndex) {
        pushIfDebugForBranch(context, groupIndex, branchIndex, "after");
    }

    @Override
    public void beforeCapabilityNode(ExecutionContext context, String capabilityName, String capabilityNodeId) {
        pushIfDebugForCapabilityNode(context, capabilityName, capabilityNodeId, "before");
    }

    @Override
    public void afterCapabilityNode(ExecutionContext context, String capabilityName, String capabilityNodeId) {
        pushIfDebugForCapabilityNode(context, capabilityName, capabilityNodeId, "after");
    }

    private void pushIfDebugForCapabilityNode(ExecutionContext context, String capabilityName, String capabilityNodeId, String phase) {
        if (context == null || capabilityNodeId == null || capabilityNodeId.isEmpty()) return;
        String debugID = getString(context, STATE_KEY_DEBUG_ID);
        Object stubObj = context.get(STATE_KEY_DEBUG_PUSH_ACTIVITY);
        if (debugID == null || debugID.isEmpty() || !(stubObj instanceof DebugPushActivity)) return;
        try {
            pushPayload(context, debugID, capabilityNodeId, (DebugPushActivity) stubObj);
        } catch (Exception e) {
            log.warn("Debug push to Redis failed (debugID={}, capability={})", debugID, capabilityName, e);
        }
    }

    private void pushIfDebug(CapabilityContext ctx, String phase) {
        ExecutionContext executionContext = ctx != null ? ctx.getExecutionContext() : null;
        if (executionContext == null) return;

        String debugID = getString(executionContext, STATE_KEY_DEBUG_ID);
        Object stubObj = executionContext.get(STATE_KEY_DEBUG_PUSH_ACTIVITY);
        if (debugID == null || debugID.isEmpty() || !(stubObj instanceof DebugPushActivity)) return;

        try {
            String nodeKind = getString(executionContext, STATE_KEY_DEBUG_NODE_KIND);
            if (nodeKind == null || nodeKind.isEmpty()) nodeKind = NODE_KIND_PLUGIN;
            String executionNodeId = ctx != null && ctx.getCapabilityDefinition() != null && ctx.getCapabilityDefinition().getPluginNodeId() != null
                    ? ctx.getCapabilityDefinition().getPluginNodeId()
                    : executionNodeUuidForCapability(nodeKind, ctx, phase);
            pushPayload(executionContext, debugID, executionNodeId, (DebugPushActivity) stubObj);
        } catch (Exception e) {
            log.warn("Debug push to Redis failed (debugID={})", debugID, e);
        }
    }

    private void pushIfDebugForGroup(ExecutionContext context, int groupIndex, CapabilityGroupSpec spec, String phase) {
        String debugID = getString(context, STATE_KEY_DEBUG_ID);
        Object stubObj = context.get(STATE_KEY_DEBUG_PUSH_ACTIVITY);
        if (debugID == null || debugID.isEmpty() || !(stubObj instanceof DebugPushActivity)) return;
        try {
            long stepId = context.getVersionedState() != null ? context.getVersionedState().getStepId() : -1L;
            String executionNodeId = spec != null && spec.getGroupNodeId() != null && !spec.getGroupNodeId().isEmpty()
                    ? spec.getGroupNodeId()
                    : executionNodeUuidForGroup(groupIndex, stepId, phase);
            pushPayload(context, debugID, executionNodeId, (DebugPushActivity) stubObj);
        } catch (Exception e) {
            log.warn("Debug push to Redis failed (debugID={})", debugID, e);
        }
    }

    private void pushIfDebugForBranch(ExecutionContext context, int groupIndex, int branchIndex, String phase) {
        String debugID = getString(context, STATE_KEY_DEBUG_ID);
        Object stubObj = context.get(STATE_KEY_DEBUG_PUSH_ACTIVITY);
        if (debugID == null || debugID.isEmpty() || !(stubObj instanceof DebugPushActivity)) return;
        try {
            long stepId = context.getVersionedState() != null ? context.getVersionedState().getStepId() : -1L;
            String executionNodeId = executionNodeUuidForExpression(groupIndex, branchIndex, stepId, phase);
            pushPayload(context, debugID, executionNodeId, (DebugPushActivity) stubObj);
        } catch (Exception e) {
            log.warn("Debug push to Redis failed (debugID={})", debugID, e);
        }
    }

    private void pushPayload(ExecutionContext executionContext, String debugID, String executionNodeId, DebugPushActivity stub) throws Exception {
        CapabilityPlan plan = getPlan(executionContext);
        String planJson = plan != null ? ExecutionModelSerde.serializePlan(plan) : "null";
        ObjectNode ctxNode = ExecutionModelSerde.objectMapper().createObjectNode();
        ctxNode.set("command", ExecutionModelSerde.objectMapper().valueToTree(executionContext.getCommand()));
        ctxNode.set("versionedState", ExecutionModelSerde.objectMapper().valueToTree(executionContext.getVersionedState()));
        String contextJson = ExecutionModelSerde.objectMapper().writeValueAsString(ctxNode);
        stub.push(debugID, executionNodeId, planJson, contextJson);
    }

    /** Unique UUID for group node (deterministic for replay). */
    private static String executionNodeUuidForGroup(int groupIndex, long stepId, String phase) {
        String key = NODE_KIND_GROUP + "|" + groupIndex + "|" + stepId + "|" + (phase != null ? phase : "");
        return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8)).toString();
    }

    /** Unique UUID for plugin or condition capability node. */
    private static String executionNodeUuidForCapability(String nodeKind, CapabilityContext ctx, String phase) {
        int groupIndex = ctx != null ? ctx.getGroupIndex() : -1;
        long stepId = ctx != null ? ctx.getStepId() : -1L;
        String capabilityName = ctx != null && ctx.getCapabilityDefinition() != null ? ctx.getCapabilityDefinition().getName() : "";
        String kind = (nodeKind != null && NODE_KIND_CONDITION.equals(nodeKind)) ? NODE_KIND_CONDITION : NODE_KIND_PLUGIN;
        String key = kind + "|" + groupIndex + "|" + capabilityName + "|" + stepId + "|" + (phase != null ? phase : "");
        return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8)).toString();
    }

    /** Unique UUID for expression (branch) node. */
    private static String executionNodeUuidForExpression(int groupIndex, int branchIndex, long stepId, String phase) {
        String key = NODE_KIND_EXPRESSION + "|" + groupIndex + "|" + branchIndex + "|" + stepId + "|" + (phase != null ? phase : "");
        return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8)).toString();
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
