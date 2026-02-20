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
package com.openllmorchestrator.worker.engine.capability;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Collections;
import java.util.List;

/** Runtime retry options for an activity. Serializable; built from config in engine layer. */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CapabilityRetryOptions {
    private int maximumAttempts;
    private int initialIntervalSeconds;
    private double backoffCoefficient;
    private int maximumIntervalSeconds;
    private List<String> nonRetryableErrors;

    public List<String> getNonRetryableErrors() {
        if (nonRetryableErrors == null || nonRetryableErrors.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(nonRetryableErrors);
    }
}
