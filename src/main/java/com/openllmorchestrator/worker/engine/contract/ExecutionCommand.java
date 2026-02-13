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
}
