package com.openllmorchestrator.worker.workflow.contract;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RagResponse {

    private String answer;

    // Optional but very useful
    private String modelUsed;

    private List<String> sources;

    private Long latencyMs;

    private Integer promptTokens;

    private Integer completionTokens;
}
