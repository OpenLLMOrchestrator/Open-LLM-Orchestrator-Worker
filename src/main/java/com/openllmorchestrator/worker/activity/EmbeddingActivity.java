package com.openllmorchestrator.worker.activity;

import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface EmbeddingActivity {

    float[] embed(String text);
}
