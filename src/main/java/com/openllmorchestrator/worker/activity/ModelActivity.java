package com.openllmorchestrator.worker.activity;

import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface ModelActivity {

    String generate(String prompt);
}
