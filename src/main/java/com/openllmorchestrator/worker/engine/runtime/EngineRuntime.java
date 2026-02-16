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
package com.openllmorchestrator.worker.engine.runtime;

import com.openllmorchestrator.worker.engine.config.EngineFileConfig;
import com.openllmorchestrator.worker.engine.config.FeatureFlags;
import com.openllmorchestrator.worker.engine.plan.PlanValidator;
import com.openllmorchestrator.worker.engine.policy.BudgetGuardrailEnforcer;
import com.openllmorchestrator.worker.engine.policy.ExecutionPolicyResolver;
import com.openllmorchestrator.worker.engine.security.SecurityHardeningGate;
import com.openllmorchestrator.worker.contract.OutputContractValidator;
import com.openllmorchestrator.worker.engine.stage.StagePlan;
import com.openllmorchestrator.worker.engine.stage.resolver.StageResolver;

import java.util.Collections;
import java.util.Map;

/**
 * One-time bootstrap state: config, execution hierarchy (plan + resolver), and resolver.
 * Built once at startup and reused for the entire container lifecycle.
 * <p>
 * <b>No transactional data:</b> This runtime holds only config-derived, immutable state.
 * No workflow ID, request context, or per-execution data is stored here to avoid memory leaks.
 * Per-run state is passed as {@link com.openllmorchestrator.worker.engine.contract.ExecutionContext}
 * at execution time and is never retained by the runtime.
 */
public final class EngineRuntime {

    private static volatile EngineFileConfig config;
    private static volatile Map<String, StagePlan> stagePlansByName;
    private static volatile StageResolver stageResolver;
    private static volatile OutputContractValidator outputContractValidator;
    private static volatile FeatureFlags featureFlags;
    private static volatile ExecutionPolicyResolver executionPolicyResolver;
    private static volatile BudgetGuardrailEnforcer budgetGuardrailEnforcer;
    private static volatile SecurityHardeningGate securityHardeningGate;
    private static volatile PlanValidator planValidator;

    /** Set during bootstrap; never null after successful init. */
    public static EngineFileConfig getConfig() {
        EngineFileConfig c = config;
        if (c == null) {
            throw new IllegalStateException("Engine not bootstrapped. Call WorkerBootstrap.initialize() first.");
        }
        return c;
    }

    public static void setConfig(EngineFileConfig config) {
        EngineRuntime.config = config;
    }

    /** Execution hierarchy (plan) for default pipeline; same as getStagePlan("default"). */
    public static StagePlan getStagePlan() {
        return getStagePlan("default");
    }

    /** Execution hierarchy (plan) for the given pipeline name. Workflow payload should pass this name. */
    public static StagePlan getStagePlan(String pipelineName) {
        Map<String, StagePlan> map = stagePlansByName;
        if (map == null || map.isEmpty()) {
            throw new IllegalStateException("Engine not bootstrapped. Stage plans not set.");
        }
        String name = pipelineName != null && !pipelineName.isBlank() ? pipelineName : "default";
        StagePlan p = map.get(name);
        if (p == null) {
            throw new IllegalStateException("Unknown pipeline name: '" + name + "'. Available: " + map.keySet());
        }
        return p;
    }

    public static void setStagePlans(Map<String, StagePlan> plans) {
        EngineRuntime.stagePlansByName = plans != null ? Collections.unmodifiableMap(plans) : null;
    }

    /** Resolves predefined stages via config + plugin bucket, custom stages via custom bucket. */
    public static StageResolver getStageResolver() {
        StageResolver r = stageResolver;
        if (r == null) {
            throw new IllegalStateException("Engine not bootstrapped. Stage resolver not set.");
        }
        return r;
    }

    public static void setStageResolver(StageResolver stageResolver) {
        EngineRuntime.stageResolver = stageResolver;
    }

    /** Optional validator for stages implementing {@link com.openllmorchestrator.worker.contract.OutputContract}. */
    public static OutputContractValidator getOutputContractValidator() {
        return outputContractValidator;
    }

    public static void setOutputContractValidator(OutputContractValidator outputContractValidator) {
        EngineRuntime.outputContractValidator = outputContractValidator;
    }

    /** Feature flags loaded at bootstrap; only enabled features execute. Returns empty (all disabled) when not yet set. */
    public static FeatureFlags getFeatureFlags() {
        FeatureFlags f = featureFlags;
        return f != null ? f : FeatureFlags.fromNames(Collections.emptyList());
    }

    public static void setFeatureFlags(FeatureFlags featureFlags) {
        EngineRuntime.featureFlags = featureFlags;
    }

    /** When POLICY_ENGINE is enabled. */
    public static ExecutionPolicyResolver getExecutionPolicyResolver() {
        return executionPolicyResolver;
    }
    public static void setExecutionPolicyResolver(ExecutionPolicyResolver executionPolicyResolver) {
        EngineRuntime.executionPolicyResolver = executionPolicyResolver;
    }

    /** When BUDGET_GUARDRAIL is enabled. */
    public static BudgetGuardrailEnforcer getBudgetGuardrailEnforcer() {
        return budgetGuardrailEnforcer;
    }
    public static void setBudgetGuardrailEnforcer(BudgetGuardrailEnforcer budgetGuardrailEnforcer) {
        EngineRuntime.budgetGuardrailEnforcer = budgetGuardrailEnforcer;
    }

    /** When SECURITY_HARDENING is enabled. */
    public static SecurityHardeningGate getSecurityHardeningGate() {
        return securityHardeningGate;
    }
    public static void setSecurityHardeningGate(SecurityHardeningGate securityHardeningGate) {
        EngineRuntime.securityHardeningGate = securityHardeningGate;
    }

    /** When PLAN_SAFETY_VALIDATION is enabled. */
    public static PlanValidator getPlanValidator() {
        return planValidator;
    }
    public static void setPlanValidator(PlanValidator planValidator) {
        EngineRuntime.planValidator = planValidator;
    }

    /** Backward compatibility; set by bootstrap together with config. Prefer getConfig(). */
    @Deprecated
    public static EngineFileConfig CONFIG;
}

