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
package com.openllmorchestrator.worker.contract;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Deterministic envelope for capability execution. Standardized so kernel and interceptors can rely on
 * output, metadata, deterministic flag, and dependency refs (replay, audit, DAG).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CapabilityResult {

    private String capabilityName;
    @Deprecated
    private Map<String, Object> data;
    private Map<String, Object> output;
    private CapabilityMetadata metadata;
    private boolean deterministic;
    private List<DependencyRef> dependencies;
    private boolean requestPipelineBreak;

    public Map<String, Object> getOutput() {
        if (output != null) return output;
        return data != null ? data : Collections.emptyMap();
    }

    @Deprecated
    public Map<String, Object> getData() {
        return getOutput();
    }

    public static CapabilityResult withOutput(String capabilityName, Map<String, Object> output) {
        return CapabilityResult.builder()
                .capabilityName(capabilityName)
                .output(output != null ? output : Map.of())
                .data(output)
                .deterministic(false)
                .dependencies(List.of())
                .build();
    }

    public static CapabilityResult deterministicEnvelope(String capabilityName, Map<String, Object> output,
                                                         CapabilityMetadata metadata, List<DependencyRef> dependencies) {
        return CapabilityResult.builder()
                .capabilityName(capabilityName)
                .output(output != null ? output : Map.of())
                .data(output)
                .metadata(metadata)
                .deterministic(true)
                .dependencies(dependencies != null ? dependencies : List.of())
                .build();
    }
}
