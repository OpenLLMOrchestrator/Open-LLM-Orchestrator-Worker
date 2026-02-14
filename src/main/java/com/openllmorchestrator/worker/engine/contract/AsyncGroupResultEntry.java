package com.openllmorchestrator.worker.engine.contract;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/** One async group result: activity name + stage result. Used as activity argument for merge policy. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AsyncGroupResultEntry implements Serializable {
    private String activityName;
    private StageResult result;
}
