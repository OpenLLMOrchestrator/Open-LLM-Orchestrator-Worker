package com.openllmorchestrator.worker.engine.contract;

import lombok.*;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StageResult {

    private String stageName;
    private Map<String, Object> data;
}
