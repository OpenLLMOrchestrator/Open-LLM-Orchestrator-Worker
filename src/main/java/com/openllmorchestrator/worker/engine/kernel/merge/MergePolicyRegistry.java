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

import java.util.HashMap;
import java.util.Map;

/**
 * Registry of async merge policies by name. Used to resolve asyncOutputMergePolicy from config;
 * custom policies can be registered and referenced in pipeline/group config.
 */
public final class MergePolicyRegistry {

    private static final MergePolicyRegistry DEFAULT = createDefault();

    private final Map<String, AsyncMergePolicy> byName = new HashMap<>();

    public MergePolicyRegistry() {}

    /** Default registry with built-in policies: FIRST_WINS, LAST_WINS, PREFIX_BY_ACTIVITY. */
    public static MergePolicyRegistry getDefault() {
        return DEFAULT;
    }

    public static MergePolicyRegistry createDefault() {
        MergePolicyRegistry r = new MergePolicyRegistry();
        r.register("FIRST_WINS", AsyncOutputMergePolicy.FIRST_WINS);
        r.register("FIRST_FINISHED_FIRST", AsyncOutputMergePolicy.FIRST_WINS);
        r.register("LAST_WINS", AsyncOutputMergePolicy.LAST_WINS);
        r.register("LAST_FINISHED_WINS", AsyncOutputMergePolicy.LAST_WINS);
        r.register("FIRST_FINISHED_LAST", AsyncOutputMergePolicy.LAST_WINS);
        r.register("PREFIX_BY_ACTIVITY", AsyncOutputMergePolicy.PREFIX_BY_ACTIVITY);
        r.register("PREFIX", AsyncOutputMergePolicy.PREFIX_BY_ACTIVITY);
        return r;
    }

    public void register(String name, AsyncMergePolicy policy) {
        if (name != null && !name.isBlank() && policy != null) {
            byName.put(name.toUpperCase(), policy);
        }
    }

    public AsyncMergePolicy get(String name) {
        if (name == null || name.isBlank()) {
            return AsyncOutputMergePolicy.LAST_WINS;
        }
        return byName.getOrDefault(name.toUpperCase(), AsyncOutputMergePolicy.LAST_WINS);
    }
}
