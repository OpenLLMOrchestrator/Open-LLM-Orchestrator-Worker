/*
 * Copyright 2026 Open LLM Orchestrator contributors.
 */
package com.openllmorchestrator.worker.sample;

import com.openllmorchestrator.worker.contract.ContractCompatibility;
import com.openllmorchestrator.worker.contract.PlannerInputDescriptor;
import com.openllmorchestrator.worker.contract.PluginContext;
import com.openllmorchestrator.worker.contract.PluginTypeDescriptor;
import com.openllmorchestrator.worker.contract.PluginTypes;
import com.openllmorchestrator.worker.contract.CapabilityHandler;
import com.openllmorchestrator.worker.contract.CapabilityResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Stub retrieval plugin: returns one fake chunk from question. For demos and contract-only plugins module. */
public final class StubRetrievalPlugin implements CapabilityHandler, ContractCompatibility, PlannerInputDescriptor, PluginTypeDescriptor {

    private static final String CONTRACT_VERSION = "0.0.1";
    public static final String NAME = "com.openllmorchestrator.worker.sample.StubRetrievalPlugin";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public CapabilityResult execute(PluginContext context) {
        Map<String, Object> input = context.getOriginalInput();
        Map<String, Object> accumulated = context.getAccumulatedOutput();
        Object chunksIn = accumulated.get("tokenizedChunks");
        if (chunksIn instanceof List && !((List<?>) chunksIn).isEmpty()) {
            context.putOutput("stored", true);
            context.putOutput("chunkCount", ((List<?>) chunksIn).size());
            return CapabilityResult.builder().capabilityName(NAME).output(new HashMap<>(context.getCurrentPluginOutput())).build();
        }
        String question = (String) input.get("question");
        if (question != null && !question.isBlank()) {
            context.putOutput("retrievedChunks", List.of(Map.of("text", "[stub] " + question, "index", 0)));
        }
        return CapabilityResult.builder().capabilityName(NAME).output(new HashMap<>(context.getCurrentPluginOutput())).build();
    }

    @Override
    public String getRequiredContractVersion() {
        return CONTRACT_VERSION;
    }

    @Override
    public Set<String> getRequiredInputFieldsForPlanner() {
        return Set.of("question", "tokenizedChunks");
    }

    @Override
    public String getPlannerDescription() {
        return "Vector store (stub): store chunks or retrieve by question.";
    }

    @Override
    public String getPluginType() {
        return PluginTypes.VECTOR_STORE;
    }
}
