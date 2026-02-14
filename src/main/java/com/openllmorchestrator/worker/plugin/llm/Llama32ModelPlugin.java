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
 * LLM plugin for Llama 3.2 via Ollama. Base URL and model from env: OLLAMA_BASE_URL, OLLAMA_MODEL.
 * Consumes retrieved context and question, returns response.
 */
public final class Llama32ModelPlugin implements StageHandler {

    public static final String NAME = "Llama32ModelPlugin";
    private static final String OLLAMA_BASE = getEnv("OLLAMA_BASE_URL", "http://localhost:11434");
    private static final String MODEL = getEnv("OLLAMA_MODEL", "llama3.2:latest");
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
        Map<String, Object> accumulated = context.getAccumulatedOutput();

        String question = (String) input.get("question");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> chunks = (List<Map<String, Object>>) accumulated.get("retrievedChunks");

        String response = callLlama32(question, chunks);
        context.putOutput("response", response);
        context.putOutput("result", response);

        return StageResult.builder().stageName(NAME).data(new HashMap<>(context.getCurrentPluginOutput())).build();
    }

    private String callLlama32(String question, List<Map<String, Object>> contextChunks) {
        if (question == null || question.isBlank()) {
            return "";
        }
        String prompt = buildPrompt(question, contextChunks);
        try {
            Map<String, Object> body = Map.of(
                    "model", MODEL,
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
