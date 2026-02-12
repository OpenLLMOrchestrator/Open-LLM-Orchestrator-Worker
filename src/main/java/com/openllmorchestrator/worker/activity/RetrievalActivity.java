package com.openllmorchestrator.worker.activity;

import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface RetrievalActivity {

    String retrieve(String documentId, float[] vector);
}
