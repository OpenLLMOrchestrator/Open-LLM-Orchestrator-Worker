package com.openllmorchestrator.worker.activity;

import io.temporal.activity.ActivityInterface;

import java.util.List;

@ActivityInterface
public interface RetrievalActivity {

    List<String> retrieve(String documentId, List<Double> vector);
}
