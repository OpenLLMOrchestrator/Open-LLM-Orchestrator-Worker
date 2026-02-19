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

import com.openllmorchestrator.worker.contract.OutputContractValidator;
import com.openllmorchestrator.worker.engine.capability.CapabilityPlan;
import com.openllmorchestrator.worker.engine.capability.resolver.CapabilityResolver;
import com.openllmorchestrator.worker.engine.config.EngineFileConfig;
import com.openllmorchestrator.worker.engine.config.FeatureFlags;
import com.openllmorchestrator.worker.engine.kernel.feature.FeatureExecutionPluginRegistry;
import com.openllmorchestrator.worker.engine.kernel.feature.FeatureHandlerRegistry;
import com.openllmorchestrator.worker.engine.kernel.interceptor.ExecutionInterceptorChain;
import com.openllmorchestrator.worker.engine.plan.PlanValidator;
import com.openllmorchestrator.worker.engine.policy.BudgetGuardrailEnforcer;
import com.openllmorchestrator.worker.engine.policy.ExecutionPolicyResolver;
import com.openllmorchestrator.worker.engine.security.SecurityHardeningGate;

import java.util.Collections;
import java.util.Map;

/**
 * Per-queue execution tree: config, plans, resolver, feature flags, feature handlers, and interceptor chain.
 * Built at bootstrap for each task queue so different queues can use different templates/pipelines.
 */
public final class QueueExecutionTree {

    private volatile EngineFileConfig config;
    private volatile Map<String, CapabilityPlan> capabilityPlansByName;
    private volatile CapabilityResolver capabilityResolver;
    private volatile OutputContractValidator outputContractValidator;
    private volatile FeatureFlags featureFlags;
    private volatile ExecutionPolicyResolver executionPolicyResolver;
    private volatile BudgetGuardrailEnforcer budgetGuardrailEnforcer;
    private volatile SecurityHardeningGate securityHardeningGate;
    private volatile PlanValidator planValidator;
    private volatile FeatureHandlerRegistry featureHandlerRegistry;
    private volatile ExecutionInterceptorChain executionInterceptorChain;
    private volatile FeatureExecutionPluginRegistry featureExecutionPluginRegistry;

    public QueueExecutionTree() {}

    public EngineFileConfig getConfig() { return config; }
    public void setConfig(EngineFileConfig config) { this.config = config; }

    public Map<String, CapabilityPlan> getCapabilityPlansByName() { return capabilityPlansByName; }
    public void setCapabilityPlans(Map<String, CapabilityPlan> plans) {
        this.capabilityPlansByName = plans != null ? Collections.unmodifiableMap(plans) : null;
    }

    public CapabilityResolver getCapabilityResolver() { return capabilityResolver; }
    public void setCapabilityResolver(CapabilityResolver capabilityResolver) { this.capabilityResolver = capabilityResolver; }

    public OutputContractValidator getOutputContractValidator() { return outputContractValidator; }
    public void setOutputContractValidator(OutputContractValidator outputContractValidator) { this.outputContractValidator = outputContractValidator; }

    public FeatureFlags getFeatureFlags() { return featureFlags; }
    public void setFeatureFlags(FeatureFlags featureFlags) { this.featureFlags = featureFlags; }

    public ExecutionPolicyResolver getExecutionPolicyResolver() { return executionPolicyResolver; }
    public void setExecutionPolicyResolver(ExecutionPolicyResolver executionPolicyResolver) { this.executionPolicyResolver = executionPolicyResolver; }

    public BudgetGuardrailEnforcer getBudgetGuardrailEnforcer() { return budgetGuardrailEnforcer; }
    public void setBudgetGuardrailEnforcer(BudgetGuardrailEnforcer budgetGuardrailEnforcer) { this.budgetGuardrailEnforcer = budgetGuardrailEnforcer; }

    public SecurityHardeningGate getSecurityHardeningGate() { return securityHardeningGate; }
    public void setSecurityHardeningGate(SecurityHardeningGate securityHardeningGate) { this.securityHardeningGate = securityHardeningGate; }

    public PlanValidator getPlanValidator() { return planValidator; }
    public void setPlanValidator(PlanValidator planValidator) { this.planValidator = planValidator; }

    public FeatureHandlerRegistry getFeatureHandlerRegistry() { return featureHandlerRegistry; }
    public void setFeatureHandlerRegistry(FeatureHandlerRegistry featureHandlerRegistry) { this.featureHandlerRegistry = featureHandlerRegistry; }

    public ExecutionInterceptorChain getExecutionInterceptorChain() { return executionInterceptorChain; }
    public void setExecutionInterceptorChain(ExecutionInterceptorChain executionInterceptorChain) { this.executionInterceptorChain = executionInterceptorChain; }

    public FeatureExecutionPluginRegistry getFeatureExecutionPluginRegistry() { return featureExecutionPluginRegistry; }
    public void setFeatureExecutionPluginRegistry(FeatureExecutionPluginRegistry featureExecutionPluginRegistry) { this.featureExecutionPluginRegistry = featureExecutionPluginRegistry; }
}
