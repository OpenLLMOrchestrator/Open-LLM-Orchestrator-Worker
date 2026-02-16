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
 * ASYNC: prefix each key with the activity/source id to avoid overwrites and data loss.
 * Example: sourceId "MemoryPlugin", key "result" → accumulated key "MemoryPlugin.result".
 */
public final class PrefixByActivityMergePolicy implements OutputMergePolicy {

    public static final String DEFAULT_SEPARATOR = ".";

    private final String separator;

    public PrefixByActivityMergePolicy() {
        this(DEFAULT_SEPARATOR);
    }

    public PrefixByActivityMergePolicy(String separator) {
        this.separator = separator != null && !separator.isEmpty() ? separator : DEFAULT_SEPARATOR;
    }

    @Override
    public void merge(Map<String, Object> accumulated, Map<String, Object> pluginOutput, String sourceId) {
        if (pluginOutput == null) return;
        String prefix = (sourceId != null && !sourceId.isEmpty() ? sourceId + separator : "");
        for (Map.Entry<String, Object> e : pluginOutput.entrySet()) {
            accumulated.put(prefix + e.getKey(), e.getValue());
        }
    }
}

