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
package com.openllmorchestrator.worker.engine.contract;

import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Execution graph for capability ordering. Replaces linear capabilityOrder with a DAG-capable structure.
 * Backward compatibility: a linear list auto-converts to a simple chain (nodes in order, edges A→B→C).
 */
@Getter
@Builder
public class ExecutionGraph {

    /** Node id → node. */
    private final Map<String, StageNode> nodes;
    /** Node id → list of successor node ids (edges). */
    private final Map<String, List<String>> edges;

    /**
     * Build a linear graph from an ordered list (backward compatibility).
     * Each stage is a node; edges are stage[i] → stage[i+1].
     */
    public static ExecutionGraph fromLinearOrder(List<String> capabilityOrder) {
        if (capabilityOrder == null || capabilityOrder.isEmpty()) {
            return ExecutionGraph.builder()
                    .nodes(Collections.emptyMap())
                    .edges(Collections.emptyMap())
                    .build();
        }
        Map<String, StageNode> nodes = new LinkedHashMap<>();
        Map<String, List<String>> edges = new LinkedHashMap<>();
        for (String name : capabilityOrder) {
            nodes.put(name, StageNode.of(name));
        }
        for (int i = 0; i < capabilityOrder.size() - 1; i++) {
            String from = capabilityOrder.get(i);
            String to = capabilityOrder.get(i + 1);
            edges.computeIfAbsent(from, k -> new ArrayList<>()).add(to);
        }
        return ExecutionGraph.builder()
                .nodes(Collections.unmodifiableMap(nodes))
                .edges(Collections.unmodifiableMap(edges))
                .build();
    }

    /**
     * Returns a valid execution order (topological order). For a linear graph this is the list order;
     * for a DAG this is one valid topological sort.
     */
    public List<String> topologicalOrder() {
        if (nodes == null || nodes.isEmpty()) {
            return List.of();
        }
        Map<String, Integer> inDegree = new LinkedHashMap<>();
        for (String id : nodes.keySet()) {
            inDegree.put(id, 0);
        }
        if (edges != null) {
            for (List<String> tos : edges.values()) {
                for (String to : tos) {
                    inDegree.merge(to, 1, Integer::sum);
                }
            }
        }
        List<String> order = new ArrayList<>();
        List<String> roots = new ArrayList<>();
        for (Map.Entry<String, Integer> e : inDegree.entrySet()) {
            if (e.getValue() == 0) {
                roots.add(e.getKey());
            }
        }
        while (!roots.isEmpty()) {
            String n = roots.remove(0);
            order.add(n);
            List<String> successors = edges != null ? edges.get(n) : null;
            if (successors != null) {
                for (String s : successors) {
                    int d = inDegree.getOrDefault(s, 0) - 1;
                    inDegree.put(s, d);
                    if (d == 0) {
                        roots.add(s);
                    }
                }
            }
        }
        if (order.size() != nodes.size()) {
            throw new IllegalStateException("ExecutionGraph has a cycle; cannot compute topological order.");
        }
        return order;
    }

    /** Export to DOT (Graphviz). When EXECUTION_GRAPH_EXPORT is enabled. */
    public String toDot() {
        StringBuilder sb = new StringBuilder("digraph G {\n");
        if (nodes != null) {
            for (String id : nodes.keySet()) {
                StageNode n = nodes.get(id);
                String label = n != null && n.getCapabilityBucketName() != null ? n.getCapabilityBucketName() : id;
                sb.append("  \"").append(escapeDot(id)).append("\" [label=\"").append(escapeDot(label)).append("\"];\n");
            }
        }
        if (edges != null) {
            for (Map.Entry<String, List<String>> e : edges.entrySet()) {
                String from = e.getKey();
                for (String to : e.getValue()) {
                    sb.append("  \"").append(escapeDot(from)).append("\" -> \"").append(escapeDot(to)).append("\";\n");
                }
            }
        }
        sb.append("}\n");
        return sb.toString();
    }

    /** Export to Mermaid (graph). When EXECUTION_GRAPH_EXPORT is enabled. */
    public String toMermaid() {
        StringBuilder sb = new StringBuilder("graph LR\n");
        if (edges != null) {
            for (Map.Entry<String, List<String>> e : edges.entrySet()) {
                String from = escapeMermaid(e.getKey());
                for (String to : e.getValue()) {
                    sb.append("  ").append(from).append("-->").append(escapeMermaid(to)).append("\n");
                }
            }
        }
        return sb.toString();
    }

    /** Export to JSON-like structure for UI: nodes and edges arrays. */
    public Map<String, Object> toJsonForUi() {
        Map<String, Object> out = new LinkedHashMap<>();
        List<Map<String, String>> nodeList = new ArrayList<>();
        if (nodes != null) {
            for (Map.Entry<String, StageNode> e : nodes.entrySet()) {
                Map<String, String> n = new LinkedHashMap<>();
                n.put("id", e.getKey());
                n.put("capabilityBucketName", e.getValue() != null ? e.getValue().getCapabilityBucketName() : e.getKey());
                nodeList.add(n);
            }
        }
        out.put("nodes", nodeList);
        List<Map<String, String>> edgeList = new ArrayList<>();
        if (edges != null) {
            for (Map.Entry<String, List<String>> e : edges.entrySet()) {
                for (String to : e.getValue()) {
                    Map<String, String> edge = new LinkedHashMap<>();
                    edge.put("from", e.getKey());
                    edge.put("to", to);
                    edgeList.add(edge);
                }
            }
        }
        out.put("edges", edgeList);
        return out;
    }

    private static String escapeDot(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String escapeMermaid(String s) {
        if (s == null) return "N";
        return s.replaceAll("[^A-Za-z0-9_]", "_");
    }
}

