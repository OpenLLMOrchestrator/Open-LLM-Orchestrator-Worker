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
 *   <li>PLANNER capability: a plugin (e.g. LLM planner) builds a {@link com.openllmorchestrator.worker.engine.capability.CapabilityPlan}
 *       using {@link com.openllmorchestrator.worker.engine.capability.CapabilityPlanBuilder} or
 *       {@link com.openllmorchestrator.worker.engine.capability.plan.CapabilityPlanFactory} and stores it in context
 *       under {@link #KEY_DYNAMIC_PLAN} via {@code context.putOutput(KEY_DYNAMIC_PLAN, plan)} (for PLAN_EXECUTOR sub-plan).</li>
 *   <li>To replace the <b>main</b> execution plan for this run (planner/debug only): get current plan (from
 *       {@link com.openllmorchestrator.worker.engine.contract.ExecutionContext#getExecutionPlan()} if set, else from
 *       EngineRuntime for this queue/pipeline), create a copy with {@code plan.copyForExecution()}, modify, then
 *       {@link com.openllmorchestrator.worker.engine.contract.ExecutionContext#setExecutionPlan(com.openllmorchestrator.worker.engine.capability.CapabilityPlan)}.
 *       The copy is created only in planner/debug phase; static flow keeps this null and uses the immutable global tree.</li>
 *   <li>PLAN_EXECUTOR capability: not a plugin; the kernel reads the plan from accumulated output under this key
 *       and executes it (when used inside an iterator, acts as an iterative plan executor).</li>
 * </ul>
 */
public final class PlannerContextKeys {

    /** Key in plugin output / accumulated output where the PLANNER plugin stores the dynamic {@link com.openllmorchestrator.worker.engine.capability.CapabilityPlan}. */
    public static final String KEY_DYNAMIC_PLAN = "dynamicPlan";

    private PlannerContextKeys() {}
}

