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
package com.openllmorchestrator.worker.plugin.llm;

/** Fixed-model chat plugin for qwen2:1.5b. Used in query-all-models ASYNC pipeline. */
public final class Qwen2_1_5bChatPlugin extends FixedModelChatPlugin {
    public static final String NAME = "Qwen2_1_5bChatPlugin";
    @Override public String name() { return NAME; }
    @Override protected String getModelId() { return "qwen2:1.5b"; }
    @Override protected String getModelLabel() { return "qwen2:1.5b"; }
}
