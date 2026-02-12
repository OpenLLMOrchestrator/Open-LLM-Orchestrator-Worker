package com.openllmorchestrator.worker.activity;

import com.openllmorchestrator.worker.activity.model.ModelResult;
import io.temporal.activity.ActivityInterface;

import java.util.List;
@ActivityInterface
public interface ModelActivity {

    ModelResult generateAnswer(String question, List<String> context);
}
