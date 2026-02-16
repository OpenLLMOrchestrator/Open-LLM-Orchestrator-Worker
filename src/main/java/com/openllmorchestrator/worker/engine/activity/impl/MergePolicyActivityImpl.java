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
package com.openllmorchestrator.worker.engine.activity.impl;

import com.openllmorchestrator.worker.engine.activity.MergePolicyActivity;
import com.openllmorchestrator.worker.engine.contract.AsyncGroupResultEntry;
import com.openllmorchestrator.worker.engine.contract.ExecutionContext;
import com.openllmorchestrator.worker.contract.StageResult;
import com.openllmorchestrator.worker.engine.runtime.EngineRuntime;
import com.openllmorchestrator.worker.contract.StageHandler;
import com.openllmorchestrator.worker.engine.stage.predefined.PredefinedStages;
import com.openllmorchestrator.worker.engine.stage.resolver.StageResolver;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Runs the merge policy plugin (by name) as an activity. Resolves the name to a StageHandler
 * from the same registry as other plugins; the handler receives accumulated output and
 * async results in context and writes the merged map to currentPluginOutput.
 */
@Slf4j
public class MergePolicyActivityImpl implements MergePolicyActivity {

    @Override
    public Map<String, Object> merge(String mergePolicyName,
                                     Map<String, Object> originalInput,
                                     Map<String, Object> accumulatedOutput,
                                     List<AsyncGroupResultEntry> asyncResults) {
        if (mergePolicyName == null || mergePolicyName.isBlank()) {
            mergePolicyName = "LAST_WINS";
        }
        StageResolver resolver = EngineRuntime.getStageResolver();
        StageHandler handler = resolver.resolve(mergePolicyName);
        if (handler == null) {
            if (PredefinedStages.isPredefined(mergePolicyName)) {
                throw new IllegalStateException("Merge policy '" + mergePolicyName
                        + "' is a stage name, not a merge plugin. Use a merge policy activity name (e.g. LAST_WINS, LastWinsMergePlugin).");
            }
            throw new IllegalStateException("Merge policy '" + mergePolicyName
                    + "' could not be resolved. Register it in the activity registry (e.g. LastWinsMergePlugin, FIRST_WINS).");
        }
        Map<String, Object> orig = originalInput != null ? originalInput : Collections.emptyMap();
        Map<String, Object> acc = accumulatedOutput != null ? new HashMap<>(accumulatedOutput) : new HashMap<>();
        ExecutionContext context = ExecutionContext.forActivity(orig, acc);
        context.put("asyncStageResults", asyncResults != null ? asyncResults : Collections.emptyList());
        handler.execute(context);
        return new HashMap<>(context.getCurrentPluginOutput());
    }
}

