package com.openllmorchestrator.worker.activity;

import io.temporal.activity.ActivityInterface;

import java.util.List;

@ActivityInterface
public interface EmbeddingActivity {

    List<Double> embed(String text);
}
