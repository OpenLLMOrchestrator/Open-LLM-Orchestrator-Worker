package com.openllmorchestrator.worker.activity.impl;

import com.openllmorchestrator.worker.activity.RetrievalActivity;

public class RetrievalActivityImpl implements RetrievalActivity {

    @Override
    public String retrieve(String documentId, float[] vector) {
        // TEMPORARY MOCK CONTEXT
        return "This is sample retrieved context for document " + documentId;
    }
}
