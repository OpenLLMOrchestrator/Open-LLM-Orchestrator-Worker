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
package com.openllmorchestrator.worker.plugin.vectordb;

import com.openllmorchestrator.worker.engine.contract.ExecutionContext;
import com.openllmorchestrator.worker.engine.contract.StageResult;
import com.openllmorchestrator.worker.engine.stage.StageHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Vector DB plugin: store chunks (doc pipeline) or retrieve (question pipeline). */
public final class VectorStoreRetrievalPlugin implements StageHandler {

    public static final String NAME = "VectorStoreRetrievalPlugin";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public StageResult execute(ExecutionContext context) {
        Map<String, Object> accumulated = context.getAccumulatedOutput();
        Map<String, Object> input = context.getOriginalInput();

        Object chunksObj = accumulated.get("tokenizedChunks");
        if (chunksObj instanceof List && !((List<?>) chunksObj).isEmpty()) {
            context.putOutput("stored", true);
            context.putOutput("chunkCount", ((List<?>) chunksObj).size());
            return StageResult.builder().stageName(NAME).data(new HashMap<>(context.getCurrentPluginOutput())).build();
        }

        String question = (String) input.get("question");
        if (question != null && !question.isBlank()) {
            context.putOutput("retrievedChunks", retrieveFromVectorDb(question));
        }

        return StageResult.builder().stageName(NAME).data(new HashMap<>(context.getCurrentPluginOutput())).build();
    }

    private List<Map<String, Object>> retrieveFromVectorDb(String question) {
        return new ArrayList<>();
    }
}
