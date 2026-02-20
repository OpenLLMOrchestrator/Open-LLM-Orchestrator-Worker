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
package com.openllmorchestrator.worker.engine.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.openllmorchestrator.worker.engine.capability.CapabilityPlan;
import com.openllmorchestrator.worker.engine.contract.ExecutionCommand;
import com.openllmorchestrator.worker.engine.contract.ExecutionSignal;
import com.openllmorchestrator.worker.engine.contract.KernelExecutionOutcome;
import com.openllmorchestrator.worker.engine.contract.VersionedState;

/**
 * Serialization and deserialization for execution tree and context data (JSON).
 * Use for persistence, messaging, or cross-process transfer.
 */
public final class ExecutionModelSerde {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private ExecutionModelSerde() {}

    /** Shared ObjectMapper for execution model types (read-only). */
    public static ObjectMapper objectMapper() {
        return MAPPER;
    }

    // --- CapabilityPlan ---
    public static String serializePlan(CapabilityPlan plan) throws JsonProcessingException {
        return MAPPER.writeValueAsString(plan);
    }

    public static CapabilityPlan deserializePlan(String json) throws JsonProcessingException {
        return MAPPER.readValue(json, CapabilityPlan.class);
    }

    public static byte[] serializePlanToBytes(CapabilityPlan plan) throws JsonProcessingException {
        return MAPPER.writeValueAsBytes(plan);
    }

    public static CapabilityPlan deserializePlan(byte[] json) throws IOException {
        return MAPPER.readValue(json, CapabilityPlan.class);
    }

    // --- ExecutionCommand ---
    public static String serializeCommand(ExecutionCommand command) throws JsonProcessingException {
        return MAPPER.writeValueAsString(command);
    }

    public static ExecutionCommand deserializeCommand(String json) throws JsonProcessingException {
        return MAPPER.readValue(json, ExecutionCommand.class);
    }

    // --- VersionedState ---
    public static String serializeVersionedState(VersionedState state) throws JsonProcessingException {
        return MAPPER.writeValueAsString(state);
    }

    public static VersionedState deserializeVersionedState(String json) throws JsonProcessingException {
        return MAPPER.readValue(json, VersionedState.class);
    }

    // --- ExecutionSignal ---
    public static String serializeSignal(ExecutionSignal signal) throws JsonProcessingException {
        return MAPPER.writeValueAsString(signal);
    }

    public static ExecutionSignal deserializeSignal(String json) throws JsonProcessingException {
        return MAPPER.readValue(json, ExecutionSignal.class);
    }

    // --- KernelExecutionOutcome ---
    public static String serializeOutcome(KernelExecutionOutcome outcome) throws JsonProcessingException {
        return MAPPER.writeValueAsString(outcome);
    }

    public static KernelExecutionOutcome deserializeOutcome(String json) throws JsonProcessingException {
        return MAPPER.readValue(json, KernelExecutionOutcome.class);
    }
}
