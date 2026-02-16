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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openllmorchestrator.worker.contract.ContractCompatibility;
import com.openllmorchestrator.worker.contract.PluginContext;
import com.openllmorchestrator.worker.contract.PlannerInputDescriptor;
import com.openllmorchestrator.worker.contract.PluginTypeDescriptor;
import com.openllmorchestrator.worker.contract.PluginTypes;
import com.openllmorchestrator.worker.contract.StageHandler;
import com.openllmorchestrator.worker.contract.StageResult;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * RAG LLM plugin via Ollama. Supports any model: use input.modelId or pipeline name (e.g. rag-mistral).
 * Base URL from env OLLAMA_BASE_URL; default model from OLLAMA_MODEL.
 * Input: "question" (string) or "messages" (chat array); optional "modelId". Uses retrievedChunks for RAG context.
 */
public final class Llama32ModelPlugin implements StageHandler, ContractCompatibility, PlannerInputDescriptor, PluginTypeDescriptor {

    private static final String CONTRACT_VERSION = "0.0.1";
    public static final String NAME = "com.openllmorchestrator.worker.plugin.llm.Llama32ModelPlugin";
    private static final String OLLAMA_BASE = getEnv("OLLAMA_BASE_URL", "http://localhost:11434");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static String getEnv(String key, String defaultValue) {
        String v = System.getenv(key);
        if (v != null && !v.isBlank()) return v.trim();
        return System.getProperty(key, defaultValue);
    }
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public StageResult execute(PluginContext context) {
        Map<String, Object> input = context.getOriginalInput();
        Map<String, Object> accumulated = context.getAccumulatedOutput();

        String question = (String) input.get("question");
        if (question == null || question.isBlank()) {
            question = deriveQuestionFromMessages(input);
        }
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> chunks = (List<Map<String, Object>>) accumulated.get("retrievedChunks");

        String modelId = OllamaModelResolver.resolveModelId(context);
        String response = callOllama(question, chunks, modelId);
        context.putOutput("response", response);
        context.putOutput("result", response);

        return StageResult.builder().stageName(NAME).data(new HashMap<>(context.getCurrentPluginOutput())).build();
    }

    @Override
    public String getRequiredContractVersion() {
        return CONTRACT_VERSION;
    }

    @Override
    public Set<String> getRequiredInputFieldsForPlanner() {
        return Set.of("question", "messages", "modelId", "retrievedChunks");
    }

    @Override
    public String getPlannerDescription() {
        return "Model: RAG/completion via Ollama; needs question or messages, optional modelId and retrievedChunks.";
    }

    @Override
    public String getPluginType() {
        return PluginTypes.MODEL;
    }

    /** When input has "messages" (chat UI) but no "question", use the last user message content. */
    @SuppressWarnings("unchecked")
    private static String deriveQuestionFromMessages(Map<String, Object> input) {
        Object messagesObj = input.get("messages");
        if (!(messagesObj instanceof List)) return "";
        List<?> messages = (List<?>) messagesObj;
        for (int i = messages.size() - 1; i >= 0; i--) {
            Object m = messages.get(i);
            if (m instanceof Map) {
                Map<String, Object> msg = (Map<String, Object>) m;
                if ("user".equals(msg.get("role"))) {
                    Object content = msg.get("content");
                    return content != null ? content.toString().trim() : "";
                }
            }
        }
        return "";
    }

    private String callOllama(String question, List<Map<String, Object>> contextChunks, String modelId) {
        if (question == null || question.isBlank()) {
            return "";
        }
        String prompt = buildPrompt(question, contextChunks);
        try {
            Map<String, Object> body = Map.of(
                    "model", modelId,
                    "prompt", prompt,
                    "stream", false
            );
            byte[] json = MAPPER.writeValueAsBytes(body);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OLLAMA_BASE + "/api/generate"))
                    .timeout(Duration.ofSeconds(120))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(json))
                    .build();
            HttpResponse<String> resp = HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() != 200) {
                return "Error: Ollama returned " + resp.statusCode() + " – " + resp.body();
            }
            JsonNode root = MAPPER.readTree(resp.body());
            JsonNode responseNode = root.path("response");
            return responseNode.isMissingNode() ? "" : responseNode.asText();
        } catch (Exception e) {
            return "Error calling Ollama: " + e.getMessage();
        }
    }

    private static String buildPrompt(String question, List<Map<String, Object>> chunks) {
        StringBuilder sb = new StringBuilder();
        if (chunks != null && !chunks.isEmpty()) {
            sb.append("Use the following context to answer the question.\n\nContext:\n");
            for (Map<String, Object> chunk : chunks) {
                Object text = chunk != null ? (chunk.get("text") != null ? chunk.get("text") : chunk.get("content")) : null;
                if (text != null) {
                    sb.append(text).append("\n");
                }
            }
            sb.append("\n");
        }
        sb.append("Question: ").append(question).append("\n\nAnswer:");
        return sb.toString();
    }
}


