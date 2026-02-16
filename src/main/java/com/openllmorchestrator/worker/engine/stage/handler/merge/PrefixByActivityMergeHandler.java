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
package com.openllmorchestrator.worker.engine.stage.handler.merge;

import com.openllmorchestrator.worker.engine.contract.AsyncGroupResultEntry;
import com.openllmorchestrator.worker.contract.PluginContext;
import com.openllmorchestrator.worker.contract.StageHandler;
import com.openllmorchestrator.worker.contract.StageResult;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Merge policy plugin: prefix keys by activity name. Invoked as activity before exiting ASYNC group. */
public final class PrefixByActivityMergeHandler implements StageHandler {

    public static final String NAME = "PREFIX_BY_ACTIVITY";
    private static final String SEP = ".";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public StageResult execute(PluginContext context) {
        Map<String, Object> merged = merge(context);
        for (Map.Entry<String, Object> e : merged.entrySet()) {
            context.putOutput(e.getKey(), e.getValue());
        }
        return StageResult.builder().stageName(NAME).data(merged).build();
    }

    private static Map<String, Object> merge(PluginContext context) {
        Map<String, Object> acc = new HashMap<>(context.getAccumulatedOutput());
        @SuppressWarnings("unchecked")
        List<AsyncGroupResultEntry> results = (List<AsyncGroupResultEntry>) context.get("asyncStageResults");
        if (results == null) results = Collections.emptyList();
        for (AsyncGroupResultEntry entry : results) {
            Map<String, Object> data = entry.getResult() != null && entry.getResult().getData() != null
                    ? entry.getResult().getData() : Collections.emptyMap();
            String prefix = (entry.getActivityName() != null && !entry.getActivityName().isEmpty()
                    ? entry.getActivityName() + SEP : "");
            for (Map.Entry<String, Object> e : data.entrySet()) {
                acc.put(prefix + e.getKey(), e.getValue());
            }
        }
        return acc;
    }
}


