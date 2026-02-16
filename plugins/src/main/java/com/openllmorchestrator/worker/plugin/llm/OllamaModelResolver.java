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
package com.openllmorchestrator.worker.plugin.llm;

import com.openllmorchestrator.worker.contract.PluginContext;

import java.util.Map;

/**
 * Resolves the Ollama model id from (1) input.modelId, (2) pipeline name (rag-X / chat-X), (3) env OLLAMA_MODEL.
 * Pipeline names like "rag-mistral" or "chat-gemma2-2b" set the model for that pipeline.
 */
public final class OllamaModelResolver {

    private static final String DEFAULT_MODEL = getEnv("OLLAMA_MODEL", "llama3.2:latest");
    /** Request timeout for Ollama /api/generate (seconds). Use OLLAMA_TIMEOUT_SECONDS env for parallel/multi-model (e.g. query-all-models). */
    private static final int DEFAULT_OLLAMA_TIMEOUT_SECONDS = 300;

    private static String getEnv(String key, String defaultValue) {
        String v = System.getenv(key);
        if (v != null && !v.isBlank()) return v.trim();
        return System.getProperty(key, defaultValue);
    }

    /**
     * Resolve model id: input.modelId &gt; from pipeline name (rag-X / chat-X) &gt; OLLAMA_MODEL.
     */
    public static String resolveModelId(PluginContext context) {
        Map<String, Object> input = context.getOriginalInput();
        Object modelIdObj = input != null ? input.get("modelId") : null;
        if (modelIdObj instanceof String) {
            String s = ((String) modelIdObj).trim();
            if (!s.isEmpty()) return toOllamaModelTag(s);
        }
        String pipelineName = context.getPipelineName();
        if (pipelineName != null && !pipelineName.isBlank()) {
            String fromPipeline = modelIdFromPipelineName(pipelineName.trim());
            if (fromPipeline != null) return fromPipeline;
        }
        return DEFAULT_MODEL;
    }

    /** Timeout in seconds for each Ollama HTTP request (env OLLAMA_TIMEOUT_SECONDS). Default 300 for parallel/multi-model. */
    public static int getOllamaTimeoutSeconds() {
        String v = getEnv("OLLAMA_TIMEOUT_SECONDS", String.valueOf(DEFAULT_OLLAMA_TIMEOUT_SECONDS));
        try {
            int s = Integer.parseInt(v);
            return s > 0 ? s : DEFAULT_OLLAMA_TIMEOUT_SECONDS;
        } catch (NumberFormatException e) {
            return DEFAULT_OLLAMA_TIMEOUT_SECONDS;
        }
    }

    /**
     * If pipeline name is "rag-X" or "chat-X", return Ollama model id for X (e.g. gemma2-2b -&gt; gemma2:2b).
     */
    private static String modelIdFromPipelineName(String pipelineName) {
        String prefix;
        if (pipelineName.startsWith("rag-")) {
            prefix = "rag-";
        } else if (pipelineName.startsWith("chat-")) {
            prefix = "chat-";
        } else {
            return null;
        }
        String suffix = pipelineName.substring(prefix.length()).trim();
        if (suffix.isEmpty()) return null;
        return toOllamaModelTag(suffix);
    }

    /** Map pipeline-friendly tags to exact Ollama model names (as returned by ollama list). */
    private static String toOllamaModelTag(String modelKey) {
        if (modelKey == null || modelKey.isBlank()) return modelKey;
        // Already contains colon: use as-is (e.g. from input or pipeline "rag-mistral:latest")
        if (modelKey.contains(":")) return modelKey;
        // Match exact Ollama model names: name:tag
        switch (modelKey) {
            case "mistral": return "mistral:latest";
            case "llama3.2": return "llama3.2:latest";
            case "phi3": return "phi3:latest";
            case "gemma2-2b": return "gemma2:2b";
            case "qwen2-1.5b": return "qwen2:1.5b";
            default: return modelKey;
        }
    }
}

