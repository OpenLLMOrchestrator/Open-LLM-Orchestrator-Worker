package com.openllmorchestrator.worker.activity.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelResult {

    private String answer;
    private String modelName;
    private Integer promptTokens;
    private Integer completionTokens;
}
