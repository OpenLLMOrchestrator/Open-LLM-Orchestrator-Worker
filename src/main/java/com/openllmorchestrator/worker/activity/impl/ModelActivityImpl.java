package com.openllmorchestrator.worker.activity.impl;

import com.openllmorchestrator.worker.activity.ModelActivity;
import com.openllmorchestrator.worker.activity.client.OllamaClient;
import com.openllmorchestrator.worker.activity.model.ModelResult;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class ModelActivityImpl implements ModelActivity {

    private final OllamaClient client = new OllamaClient();

    @Override
    public ModelResult generateAnswer(String question, List<String> context) {

        log.info("ModelActivity started. Context size={}", context.size());

        String joinedContext = String.join("\n", context);

        String prompt = """
                Use the following context to answer the question.

                Context:
                %s

                Question:
                %s
                """.formatted(joinedContext, question);

        OllamaClient.GenerationResult result = client.generate(prompt);

        log.info("ModelActivity completed");

        return ModelResult.builder()
                .answer(result.getAnswer())
                .modelName("llama3")
                .promptTokens(result.getPromptTokens())
                .completionTokens(result.getCompletionTokens())
                .build();
    }
}



