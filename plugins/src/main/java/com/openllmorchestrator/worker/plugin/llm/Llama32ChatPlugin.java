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

/**
 * Chat LLM plugin via Ollama (no RAG). Supports any model: use input.modelId or pipeline name (e.g. chat-mistral).
 * Input: "messages" (chat array) or "question" (string). For RAG use Llama32ModelPlugin.
 * Env: OLLAMA_BASE_URL; default model OLLAMA_MODEL.
 */
public final class Llama32ChatPlugin implements StageHandler, ContractCompatibility, PlannerInputDescriptor, PluginTypeDescriptor {

    private static final String CONTRACT_VERSION = "0.0.1";
    public static final String NAME = "com.openllmorchestrator.worker.plugin.llm.Llama32ChatPlugin";
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
        String question = (String) input.get("question");
        if (question == null || question.isBlank()) {
            question = deriveQuestionFromMessages(input);
        }
        String modelId = OllamaModelResolver.resolveModelId(context);
        String response = callOllama(question, modelId);
        context.putOutput("response", response);
        context.putOutput("result", response);
        return StageResult.builder().stageName(NAME).data(new HashMap<>(context.getCurrentPluginOutput())).build();
    }

    @Override
    public String getRequiredContractVersion() {
        return CONTRACT_VERSION;
    }

    @Override
    public java.util.Set<String> getRequiredInputFieldsForPlanner() {
        return java.util.Set.of("question", "messages", "modelId");
    }

    @Override
    public String getPlannerDescription() {
        return "Model: chat via Ollama; needs question or messages.";
    }

    @Override
    public String getPluginType() {
        return PluginTypes.MODEL;
    }

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

    private String callOllama(String prompt, String modelId) {
        if (prompt == null || prompt.isBlank()) return "";
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
}


