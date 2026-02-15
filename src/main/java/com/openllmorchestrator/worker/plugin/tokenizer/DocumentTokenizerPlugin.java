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
package com.openllmorchestrator.worker.plugin.tokenizer;

import com.openllmorchestrator.worker.engine.contract.ExecutionContext;
import com.openllmorchestrator.worker.engine.contract.StageResult;
import com.openllmorchestrator.worker.engine.stage.StageHandler;

import java.util.List;
import java.util.Map;

/**
 * Document tokenizer plugin. Splits document content into chunks (e.g. for embedding and storage).
 * Intended to be moved to a separate repo later.
 */
public final class DocumentTokenizerPlugin implements StageHandler {

    public static final String NAME = "DocumentTokenizerPlugin";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public StageResult execute(ExecutionContext context) {
        Map<String, Object> input = context.getOriginalInput();

        Object docObj = input.get("document");
        String content = docObj != null ? docObj.toString() : "";
        List<Map<String, Object>> chunks = tokenize(content);

        context.putOutput("tokenizedChunks", chunks);

        return StageResult.builder().stageName(NAME).data(context.getCurrentPluginOutput()).build();
    }

    private List<Map<String, Object>> tokenize(String content) {
        // Stub: in real impl, split by sentences/paragraphs, optional tokenization, return chunk maps.
        if (content == null || content.isBlank()) {
            return List.of();
        }
        return List.of(Map.<String, Object>of("text", content, "index", 0));
    }
}
