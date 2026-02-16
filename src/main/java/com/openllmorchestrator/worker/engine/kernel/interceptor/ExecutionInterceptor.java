/*
 * Copyright 2026 Open LLM Orchestrator contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.openllmorchestrator.worker.engine.kernel.interceptor;

import com.openllmorchestrator.worker.contract.StageResult;

/**
 * Kernel-level interceptor (not a stage). Invoked before each stage, after each stage, and on error.
 * Plug in: SnapshotWriter, AuditWriter, Tracer, Evaluator, CostMeter, etc.
 */
public interface ExecutionInterceptor {

    /**
     * Called before the stage runs. State in ctx is the versioned state before this stage.
     */
    void beforeStage(StageContext ctx);

    /**
     * Called after the stage completed successfully. result is the stage output (before merge).
     */
    void afterStage(StageContext ctx, StageResult result);

    /**
     * Called when the stage threw an exception.
     */
    void onError(StageContext ctx, Exception e);
}

