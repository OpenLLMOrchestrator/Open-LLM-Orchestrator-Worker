package com.openllmorchestrator.worker.plugin.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openllmorchestrator.worker.engine.contract.ExecutionContext;
import com.openllmorchestrator.worker.engine.contract.StageResult;
import com.openllmorchestrator.worker.engine.stage.StageHandler;

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
 * Chat plugin that always uses a fixed Ollama model. Used in the "query-all-models" ASYNC pipeline
 * so each stage has one model; outputs modelLabel for the merge handler.
 */
public abstract class FixedModelChatPlugin implements StageHandler {

    private static final String OLLAMA_BASE = getEnv("OLLAMA_BASE_URL", "http://localhost:11434");
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static String getEnv(String key, String defaultValue) {
        String v = System.getenv(key);
        if (v != null && !v.isBlank()) return v.trim();
        return System.getProperty(key, defaultValue);
    }

    protected abstract String getModelId();
    protected abstract String getModelLabel();

    @Override
    public StageResult execute(ExecutionContext context) {
        Map<String, Object> input = context.getOriginalInput();
        String question = (String) input.get("question");
        if (question == null || question.isBlank()) {
            question = deriveQuestionFromMessages(input);
        }
        String response = callOllama(question, getModelId());
        context.putOutput("response", response);
        context.putOutput("result", response);
        context.putOutput("modelLabel", getModelLabel());
        return StageResult.builder().stageName(name()).data(new HashMap<>(context.getCurrentPluginOutput())).build();
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

    private static String callOllama(String prompt, String modelId) {
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
