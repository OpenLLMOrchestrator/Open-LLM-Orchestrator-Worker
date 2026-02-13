package com.openllmorchestrator.worker.engine.activity.impl;

import com.openllmorchestrator.worker.engine.activity.KernelStageActivity;
import com.openllmorchestrator.worker.engine.contract.StageResult;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class KernelStageActivityImpl implements KernelStageActivity {

    @Override
    public StageResult execute(String stageName) {

        log.info(">>> [START] Stage: {} | Thread: {}",
                stageName,
                Thread.currentThread().getName());

        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        log.info("<<< [END] Stage: {} | Thread: {}",
                stageName,
                Thread.currentThread().getName());

        return StageResult.builder()
                .stageName(stageName)
                .build();
    }
}
