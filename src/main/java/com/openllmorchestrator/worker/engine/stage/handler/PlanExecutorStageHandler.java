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
package com.openllmorchestrator.worker.engine.stage.handler;

import com.openllmorchestrator.worker.contract.PluginContext;
import com.openllmorchestrator.worker.contract.StageHandler;
import com.openllmorchestrator.worker.contract.StageResult;

/**
 * Placeholder handler for PLAN_EXECUTOR. The PLAN_EXECUTOR stage is not executed as an activity:
 * the kernel's {@link com.openllmorchestrator.worker.engine.kernel.execution.PlanExecutorGroupExecutor}
 * runs the dynamic plan from context in the workflow. This handler exists so that stage resolution
 * succeeds if the activity is ever invoked directly (e.g. from tests); it is a no-op.
 */
public final class PlanExecutorStageHandler implements StageHandler {
    public static final String NAME = "PLAN_EXECUTOR";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public StageResult execute(PluginContext context) {
        return StageResult.builder().stageName(NAME).build();
    }
}


