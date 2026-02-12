package com.openllmorchestrator.worker.workflow.contract;

import lombok.Data;

@Data
public class RagRequest {

    private String documentId;
    private String question;

    // Future-ready fields (optional later)
    // private String modelPreference;
    // private Integer maxTokens;
    // private Double temperature;
}
