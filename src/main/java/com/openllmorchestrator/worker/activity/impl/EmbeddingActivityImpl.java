package com.openllmorchestrator.worker.activity.impl;

import com.openllmorchestrator.worker.activity.EmbeddingActivity;

public class EmbeddingActivityImpl implements EmbeddingActivity {

    @Override
    public float[] embed(String text) {
        // TEMPORARY MOCK
        return new float[]{1.0f, 2.0f, 3.0f};
    }
}
