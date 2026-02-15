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
package com.openllmorchestrator.worker.engine.stage;

/** How to complete an async group. From config; no hardcoded behavior. */
public enum AsyncCompletionPolicy {
    ALL,
    FIRST_SUCCESS,
    FIRST_FAILURE,
    ALL_SETTLED;

    public static AsyncCompletionPolicy fromConfig(String value) {
        if (value == null || value.isBlank()) return ALL;
        switch (value.toUpperCase()) {
            case "FIRST_SUCCESS": return FIRST_SUCCESS;
            case "FIRST_FAILURE": return FIRST_FAILURE;
            case "ALL_SETTLED": return ALL_SETTLED;
            default: return ALL;
        }
    }
}
