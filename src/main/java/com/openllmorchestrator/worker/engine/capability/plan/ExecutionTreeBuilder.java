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

import com.openllmorchestrator.worker.engine.capability.ExecutionNodeType;
import com.openllmorchestrator.worker.engine.capability.ExecutionTreeNode;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Builds the execution tree in the same shape as config: capability → group → plugin (leaf).
 * Used when building plan from rootByCapability so executionTreeRoots have proper children.
 */
public final class ExecutionTreeBuilder {

    private static String nodeId(String kind, String... pathParts) {
        String key = kind + "|" + (pathParts != null && pathParts.length > 0 ? String.join("|", pathParts) : "");
        return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8)).toString();
    }

    private final List<ExecutionTreeNode> roots = new ArrayList<>();
    private final List<String> capabilityNodeIds = new ArrayList<>();

    /** Stack: each frame is (nodeId, type, name, mutable children list). */
    private static final class Frame {
        final String nodeId;
        final ExecutionNodeType type;
        final String name;
        final List<ExecutionTreeNode> children = new ArrayList<>();

        Frame(String nodeId, ExecutionNodeType type, String name) {
            this.nodeId = nodeId;
            this.type = type;
            this.name = name;
        }
    }

    private final List<Frame> stack = new ArrayList<>();
    private String currentCapabilityName;

    public ExecutionTreeBuilder() {}

    /** Start a capability node (e.g. MODEL, POST_PROCESS). Call before processing its group. */
    public void startCapability(String capabilityName) {
        currentCapabilityName = capabilityName;
        String id = nodeId("capability", capabilityName);
        capabilityNodeIds.add(id);
        stack.add(new Frame(id, ExecutionNodeType.CAPABILITY, capabilityName));
    }

    /** End the current capability and add it to roots. Call after processing its group. */
    public void endCapability() {
        if (stack.isEmpty() || stack.get(stack.size() - 1).type != ExecutionNodeType.CAPABILITY) {
            return;
        }
        Frame frame = stack.remove(stack.size() - 1);
        roots.add(ExecutionTreeNode.withChildren(frame.nodeId, frame.type, frame.name, new ArrayList<>(frame.children)));
    }

    /** Start a group under the current capability. Returns groupNodeId for the flat plan. */
    public String startGroup(String executionModeOrLabel) {
        String groupName = executionModeOrLabel != null && !executionModeOrLabel.isBlank() ? executionModeOrLabel : "SYNC";
        String id = nodeId("group", currentCapabilityName, groupName);
        stack.add(new Frame(id, ExecutionNodeType.GROUP, groupName));
        return id;
    }

    /** End the current group and add it to the capability's children. */
    public void endGroup() {
        if (stack.size() < 2) return;
        Frame groupFrame = stack.remove(stack.size() - 1);
        Frame parent = stack.get(stack.size() - 1);
        parent.children.add(ExecutionTreeNode.withChildren(groupFrame.nodeId, groupFrame.type, groupFrame.name, new ArrayList<>(groupFrame.children)));
    }

    /** Add a plugin (leaf) under the current group. Returns pluginNodeId for the flat plan. */
    public String addPlugin(String pluginName) {
        String id = nodeId("plugin", currentCapabilityName, pluginName);
        if (!stack.isEmpty()) {
            stack.get(stack.size() - 1).children.add(ExecutionTreeNode.of(id, ExecutionNodeType.PLUGIN, pluginName));
        }
        return id;
    }

    public List<ExecutionTreeNode> getRoots() {
        return new ArrayList<>(roots);
    }

    public List<String> getCapabilityNodeIds() {
        return new ArrayList<>(capabilityNodeIds);
    }

    /** Current group node id (when inside a group). Used by plugin processor to pass to addSyncWithCustomConfig. */
    public String getCurrentGroupNodeId() {
        for (int i = stack.size() - 1; i >= 0; i--) {
            if (stack.get(i).type == ExecutionNodeType.GROUP) {
                return stack.get(i).nodeId;
            }
        }
        return null;
    }
}
