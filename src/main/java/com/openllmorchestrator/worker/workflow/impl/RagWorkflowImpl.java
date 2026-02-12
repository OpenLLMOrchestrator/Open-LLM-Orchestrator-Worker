package com.openllmorchestrator.worker.workflow.impl;

import com.openllmorchestrator.worker.activity.EmbeddingActivity;
import com.openllmorchestrator.worker.activity.ModelActivity;
import com.openllmorchestrator.worker.activity.RetrievalActivity;
import com.openllmorchestrator.worker.activity.model.ModelResult;
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

        List<Double> vector = embeddingActivity.embed(request.getQuestion());

        List<String> context =
                retrievalActivity.retrieve(request.getDocumentId(), vector);

        String prompt = buildPrompt(context, request.getQuestion());

        ModelResult result = modelActivity.generateAnswer(
                request.getQuestion(),
                context
        );


        long latency = System.currentTimeMillis() - start;

        return RagResponse.builder()
                .answer(result.getAnswer())
                .modelUsed(result.getModelName())
                .sources(context)
                .latencyMs(latency)
                .promptTokens(result.getPromptTokens())
                .completionTokens(result.getCompletionTokens())
                .build();

    }


    private String buildPrompt(List<String> context, String question) {
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
