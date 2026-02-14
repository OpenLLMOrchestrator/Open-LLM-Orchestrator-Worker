package com.openllmorchestrator.worker.engine.contract;

import lombok.*;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionCommand {

    private String operation;
    private String tenantId;
    private String userId;
    private Map<String, Object> metadata;
    /** Initial input map for the pipeline; every plugin can read this (read-only). */
    private Map<String, Object> input;
    /** Pipeline name to run (e.g. "chat", "document-extraction"). When null/blank, "default" is used. */
    private String pipelineName;
}
