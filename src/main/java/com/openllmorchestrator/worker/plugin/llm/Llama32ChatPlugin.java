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
 * Chat LLM plugin via Ollama (no RAG). Supports any model: use input.modelId or pipeline name (e.g. chat-mistral).
 * Input: "messages" (chat array) or "question" (string). For RAG use Llama32ModelPlugin.
 * Env: OLLAMA_BASE_URL; default model OLLAMA_MODEL.
 */
public final class Llama32ChatPlugin implements StageHandler {

    public static final String NAME = "Llama32ChatPlugin";
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
    public StageResult execute(ExecutionContext context) {
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
