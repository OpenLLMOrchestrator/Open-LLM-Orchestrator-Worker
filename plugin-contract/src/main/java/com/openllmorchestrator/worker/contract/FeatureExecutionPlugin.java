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
package com.openllmorchestrator.worker.contract;

/**
 * Plugin contract for feature execution: pre and post hooks at each capability when a feature is enabled.
 * User configures one or more plugins per feature in the global config; at bootstrap they are attached
 * to the feature and called in config order with {@link FeatureExecutionContext} at every node traversal.
 */
public interface FeatureExecutionPlugin {

    /** Unique plugin name (used in config to reference this plugin). Return a constant. */
    String name();

    /** Called before the capability runs for this feature. All plugins for the feature run in config order. */
    void beforeFeature(FeatureExecutionContext ctx);

    /** Called after the capability runs for this feature. All plugins run in same config order. */
    void afterFeature(FeatureExecutionContext ctx);
}
