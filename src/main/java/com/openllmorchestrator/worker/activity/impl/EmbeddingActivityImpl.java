package com.openllmorchestrator.worker.activity.impl;

import com.openllmorchestrator.worker.activity.EmbeddingActivity;
import com.openllmorchestrator.worker.activity.client.OllamaClient;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class EmbeddingActivityImpl implements EmbeddingActivity {

    private final OllamaClient client = new OllamaClient();

    @Override
    public List<Double> embed(String text) {
        log.info("EmbeddingActivity started");
        List<Double> result = client.embed(text);
        log.info("EmbeddingActivity completed");
        return result;
    }
}

