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
package com.openllmorchestrator.worker.engine.kernel.merge;

import java.util.Map;

/**
 * ASYNC: first finished job's output is written first; keys from later completers do not overwrite.
 */
public final class FirstWriterWinsMergePolicy implements OutputMergePolicy {

    public static final FirstWriterWinsMergePolicy INSTANCE = new FirstWriterWinsMergePolicy();

    private FirstWriterWinsMergePolicy() {}

    @Override
    public void merge(Map<String, Object> accumulated, Map<String, Object> pluginOutput, String sourceId) {
        if (pluginOutput == null) return;
        for (Map.Entry<String, Object> e : pluginOutput.entrySet()) {
            accumulated.putIfAbsent(e.getKey(), e.getValue());
        }
    }
}
