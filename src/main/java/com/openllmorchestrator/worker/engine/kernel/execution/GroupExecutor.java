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
package com.openllmorchestrator.worker.engine.kernel.execution;

import com.openllmorchestrator.worker.engine.contract.ExecutionContext;
import com.openllmorchestrator.worker.engine.kernel.StageInvoker;
import com.openllmorchestrator.worker.engine.kernel.interceptor.ExecutionInterceptorChain;
import com.openllmorchestrator.worker.engine.stage.StageGroupSpec;

/** Executes one group of stages. Add new modes by adding implementations (OCP). */
public interface GroupExecutor {
    boolean supports(StageGroupSpec spec);

    /**
     * Execute the group. Interceptor chain is invoked before/after each stage and on error (non-optional).
     */
    void execute(StageGroupSpec spec, StageInvoker invoker, ExecutionContext context,
                 int groupIndex, ExecutionInterceptorChain interceptorChain);
}

