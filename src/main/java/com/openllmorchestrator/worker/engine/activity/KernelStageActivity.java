package com.openllmorchestrator.worker.engine.activity;

import com.openllmorchestrator.worker.engine.contract.StageResult;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface KernelStageActivity {

    @ActivityMethod
    StageResult execute(String stageName);
}
