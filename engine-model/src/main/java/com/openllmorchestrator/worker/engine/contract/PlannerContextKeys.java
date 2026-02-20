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
package com.openllmorchestrator.worker.engine.contract;

/**
 * Well-known keys for planner/plan-executor flow.
 * <ul>
 *   <li>PLANNER capability: a plugin (e.g. LLM planner) builds a CapabilityPlan
 *       and stores it in context under {@link #KEY_DYNAMIC_PLAN} (for PLAN_EXECUTOR sub-plan).</li>
 *   <li>To replace the <b>main</b> execution plan for this run (planner/debug only): get current plan (from
 *       ExecutionContext#getExecutionPlan() if set, else from EngineRuntime), create a copy with
 *       plan.copyForExecution(), modify, then ExecutionContext#setExecutionPlan(CapabilityPlan).
 *       The copy is created only in planner/debug phase; static flow keeps this null and uses the immutable global tree.</li>
 *   <li>PLAN_EXECUTOR capability: reads the plan from accumulated output under this key and executes it.</li>
 * </ul>
 */
public final class PlannerContextKeys {

    /** Key in plugin output / accumulated output where the PLANNER plugin stores the dynamic CapabilityPlan. */
    public static final String KEY_DYNAMIC_PLAN = "dynamicPlan";

    private PlannerContextKeys() {}
}
