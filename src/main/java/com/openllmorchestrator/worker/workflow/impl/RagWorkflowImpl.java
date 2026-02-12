package com.openllmorchestrator.worker.workflow.impl;

import com.openllmorchestrator.worker.activity.EmbeddingActivity;
import com.openllmorchestrator.worker.activity.ModelActivity;
import com.openllmorchestrator.worker.activity.RetrievalActivity;
import com.openllmorchestrator.worker.workflow.RagWorkflow;
import com.openllmorchestrator.worker.workflow.contract.RagRequest;
import com.openllmorchestrator.worker.workflow.contract.RagResponse;
import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;

import java.time.Duration;
import java.util.List;

public class RagWorkflowImpl implements RagWorkflow {

    private final ActivityOptions activityOptions =
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofSeconds(30))
                    .build();

    private final EmbeddingActivity embeddingActivity =
            Workflow.newActivityStub(EmbeddingActivity.class, activityOptions);

    private final RetrievalActivity retrievalActivity =
            Workflow.newActivityStub(RetrievalActivity.class, activityOptions);

    private final ModelActivity modelActivity =
            Workflow.newActivityStub(ModelActivity.class, activityOptions);

    @Override
    public RagResponse ask(RagRequest request) {

        long start = System.currentTimeMillis();

        float[] vector = embeddingActivity.embed(request.getQuestion());

        String context =
                retrievalActivity.retrieve(request.getDocumentId(), vector);

        String prompt = buildPrompt(context, request.getQuestion());

        String answer = modelActivity.generate(prompt);

        long latency = System.currentTimeMillis() - start;

        return RagResponse.builder()
                .answer(answer)
                .modelUsed("mock-model")
                .sources(List.of("doc-" + request.getDocumentId()))
                .latencyMs(latency)
                .promptTokens(0)
                .completionTokens(0)
                .build();
    }


    private String buildPrompt(String context, String question) {
        return """
                Use the following context to answer the question.

                Context:
                %s

                Question:
                %s

                Answer:
                """.formatted(context, question);
    }
}
