package com.openllmorchestrator.worker.activity.impl;

import com.openllmorchestrator.worker.activity.RetrievalActivity;

import java.util.List;

public class RetrievalActivityImpl implements RetrievalActivity {

    @Override
    public List<String> retrieve(String documentId, List<Double> vector) {
        // TEMPORARY MOCK CONTEXT
        return List.of(
                "Temporal is a durable execution platform for reliable distributed systems."
        );
    }
}
