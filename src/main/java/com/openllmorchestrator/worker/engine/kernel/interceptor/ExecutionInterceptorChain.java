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

import com.openllmorchestrator.worker.contract.CapabilityResult;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Non-optional snapshot interceptor layer: invokes all registered interceptors before/after each capability and on error.
 * Exceptions from one interceptor are logged and do not block others.
 */
@Slf4j
public final class ExecutionInterceptorChain implements ExecutionInterceptor {

    private final List<ExecutionInterceptor> interceptors;

    public ExecutionInterceptorChain(List<ExecutionInterceptor> interceptors) {
        this.interceptors = interceptors != null && !interceptors.isEmpty()
                ? new ArrayList<>(interceptors)
                : Collections.emptyList();
    }

    /** No-op chain when no interceptors are configured. */
    public static ExecutionInterceptorChain noOp() {
        return new ExecutionInterceptorChain(Collections.emptyList());
    }

    @Override
    public void beforeCapability(CapabilityContext ctx) {
        for (ExecutionInterceptor interceptor : interceptors) {
            try {
                interceptor.beforeCapability(ctx);
            } catch (Exception e) {
                log.warn("ExecutionInterceptor {} failed in beforeCapability: {}", interceptor.getClass().getSimpleName(), e.getMessage(), e);
            }
        }
    }

    @Override
    public void afterCapability(CapabilityContext ctx, CapabilityResult result) {
        for (ExecutionInterceptor interceptor : interceptors) {
            try {
                interceptor.afterCapability(ctx, result);
            } catch (Exception e) {
                log.warn("ExecutionInterceptor {} failed in afterCapability: {}", interceptor.getClass().getSimpleName(), e.getMessage(), e);
            }
        }
    }

    @Override
    public void onError(CapabilityContext ctx, Exception e) {
        for (ExecutionInterceptor interceptor : interceptors) {
            try {
                interceptor.onError(ctx, e);
            } catch (Exception ex) {
                log.warn("ExecutionInterceptor {} failed in onError: {}", interceptor.getClass().getSimpleName(), ex.getMessage(), ex);
            }
        }
    }
}

