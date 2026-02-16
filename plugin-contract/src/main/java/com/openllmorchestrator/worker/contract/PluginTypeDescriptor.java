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
 * Optional interface for plugins to declare their type (e.g. ToolPlugin, ModelPlugin).
 * The worker can check {@code handler instanceof PluginTypeDescriptor} and use
 * {@link #getPluginType()} to filter plugins—e.g. send only handlers of type
 * {@link PluginTypes#TOOL} as "available tools" to the planner LLM.
 * <p>
 * Use constants from {@link PluginTypes} so types stay aligned with pipeline config
 * {@code pluginType} and the worker's allowed types.
 */
public interface PluginTypeDescriptor {

    /**
     * The plugin type (e.g. {@link PluginTypes#TOOL}, {@link PluginTypes#MODEL}).
     * Should match the value used in pipeline config for this plugin's stage node.
     */
    String getPluginType();
}
