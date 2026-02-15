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
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;

/**
 * Wrapper for a dynamic plugin loaded from a JAR. If the plugin was not available at load time
 * (JAR missing or load failed), the delegate is null and execute() logs and returns an empty result
 * so the workflow continues.
 */
@Slf4j
public final class DynamicPluginWrapper implements StageHandler {

    private final String pluginName;
    private final StageHandler delegate;

    public DynamicPluginWrapper(String pluginName, StageHandler delegate) {
        this.pluginName = pluginName != null ? pluginName : "dynamic";
        this.delegate = delegate;
    }

    @Override
    public String name() {
        return pluginName;
    }

    @Override
    public StageResult execute(ExecutionContext context) {
        if (delegate == null) {
            log.warn("Dynamic plugin '{}' is not available (JAR was missing or failed to load); skipping stage and continuing.", pluginName);
            return StageResult.builder()
                    .stageName(pluginName)
                    .data(Collections.emptyMap())
                    .build();
        }
        return delegate.execute(context);
    }
}
