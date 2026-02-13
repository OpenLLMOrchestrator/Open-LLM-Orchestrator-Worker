package com.openllmorchestrator.worker.engine.contract;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ExecutionResult {

    private boolean success;
}
