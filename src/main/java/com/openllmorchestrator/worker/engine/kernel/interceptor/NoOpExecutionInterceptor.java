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
 * No-op interceptor. Use when you need a placeholder or to satisfy a non-null list.
 * For "no interceptors" use {@link ExecutionInterceptorChain#noOp()} instead.
 */
public final class NoOpExecutionInterceptor implements ExecutionInterceptor {

    public static final NoOpExecutionInterceptor INSTANCE = new NoOpExecutionInterceptor();

    private NoOpExecutionInterceptor() {}

    @Override
    public void beforeStage(StageContext ctx) {}

    @Override
    public void afterStage(StageContext ctx, StageResult result) {}

    @Override
    public void onError(StageContext ctx, Exception e) {}
}

