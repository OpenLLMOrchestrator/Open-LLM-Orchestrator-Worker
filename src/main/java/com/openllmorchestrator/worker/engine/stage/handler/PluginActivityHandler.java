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

import com.openllmorchestrator.worker.engine.contract.ExecutionContext;
import com.openllmorchestrator.worker.engine.contract.StageResult;
import com.openllmorchestrator.worker.engine.stage.StageHandler;

/**
 * StageHandler that represents one plugin/activity by name.
 * Each activity name (e.g. AccessControlPlugin) is implemented by one such handler.
 */
public final class PluginActivityHandler implements StageHandler {
    private final String activityName;

    public PluginActivityHandler(String activityName) {
        if (activityName == null || activityName.isBlank()) {
            throw new IllegalArgumentException("activityName must be non-blank");
        }
        this.activityName = activityName;
    }

    @Override
    public String name() {
        return activityName;
    }

    @Override
    public StageResult execute(ExecutionContext context) {
        return StageResult.builder().stageName(activityName).build();
    }
}
