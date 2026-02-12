package com.openllmorchestrator.worker.activity.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class OllamaClient {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
//    private final String baseUrl = "http://ollama:11434";
    private final String baseUrl = "http://localhost:11434";

    public OllamaClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public List<Double> embed(String text) {
        try {

            log.info("Calling Ollama embeddings API. Text length={}", text.length());

            Map<String, Object> payload = new HashMap<>();
            payload.put("model", "nomic-embed-text");
            payload.put("prompt", text);

            String body = objectMapper.writeValueAsString(payload);


            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/embeddings"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            log.debug("Embedding request payload: {}", body);

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            log.info("Ollama embedding response status={}", response.statusCode());
            log.debug("Ollama embedding raw response={}", response.body());

            JsonNode json = objectMapper.readTree(response.body());
            JsonNode embeddingArray = json.get("embedding");

            if (embeddingArray == null || !embeddingArray.isArray()) {
                log.error("Invalid embedding response from Ollama: {}", response.body());
                throw new RuntimeException("Invalid embedding response");
            }

            List<Double> vector = new ArrayList<>();
            embeddingArray.forEach(node -> vector.add(node.asDouble()));

            log.info("Embedding generated successfully. Vector size={}", vector.size());

            return vector;

        } catch (Exception e) {
            log.error("Embedding call failed", e);
            throw new RuntimeException("Embedding call failed", e);
        }
    }


    public GenerationResult generate(String prompt) {
        try {

            log.info("Calling Ollama generate API. Prompt length={}", prompt.length());

            Map<String, Object> payload = new HashMap<>();
            payload.put("model", "llama3");
            payload.put("prompt", prompt);
            payload.put("stream", false);

            String body = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/generate"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            log.info("Ollama generate response status={}", response.statusCode());
            log.debug("Ollama generate raw response={}", response.body());

            if (response.statusCode() != 200) {
                throw new RuntimeException("Ollama error: " + response.body());
            }

            JsonNode json = objectMapper.readTree(response.body());

            return GenerationResult.builder()
                    .answer(json.get("response").asText())
                    .promptTokens(json.has("prompt_eval_count") ? json.get("prompt_eval_count").asInt() : null)
                    .completionTokens(json.has("eval_count") ? json.get("eval_count").asInt() : null)
                    .build();

        } catch (Exception e) {
            log.error("Generate call failed", e);
            throw new RuntimeException("Generate call failed", e);
        }
    }



    @Data
    @Builder
    public static class GenerationResult {
        private String answer;
        private Integer promptTokens;
        private Integer completionTokens;
    }
}
