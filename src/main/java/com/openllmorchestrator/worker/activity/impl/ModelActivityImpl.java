package com.openllmorchestrator.worker.activity.impl;

import com.openllmorchestrator.worker.activity.ModelActivity;

public class ModelActivityImpl implements ModelActivity {

    @Override
    public String generate(String prompt) {
        // TEMPORARY MOCK RESPONSE
        return "Generated answer based on prompt:\n\n" + prompt;
    }
}
