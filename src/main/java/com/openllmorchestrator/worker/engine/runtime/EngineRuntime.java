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
import com.openllmorchestrator.worker.engine.capability.CapabilityPlan;
import com.openllmorchestrator.worker.engine.capability.resolver.CapabilityResolver;
import com.openllmorchestrator.worker.engine.kernel.feature.FeatureExecutionPluginRegistry;
import com.openllmorchestrator.worker.engine.kernel.feature.FeatureHandlerRegistry;
import com.openllmorchestrator.worker.engine.kernel.interceptor.ExecutionInterceptorChain;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-queue bootstrap state: each task queue has its own execution tree (config, plans, resolver, feature handlers).
 * Built once per queue at startup so different queues can use different templates/pipelines.
 */
public final class EngineRuntime {

    private static final ConcurrentHashMap<String, QueueExecutionTree> runtimesByQueue = new ConcurrentHashMap<>();

    /** Resolve queue name for lookup: null/blank → default (first registered or "default"). */
    public static String resolveDefaultQueue() {
        if (runtimesByQueue.isEmpty()) {
            return "default";
        }
        if (runtimesByQueue.size() == 1) {
            return runtimesByQueue.keySet().iterator().next();
        }
        String fromEnv = System.getenv("QUEUE_NAME");
        if (fromEnv != null && !fromEnv.isBlank() && runtimesByQueue.containsKey(fromEnv.trim())) {
            return fromEnv.trim();
        }
        return runtimesByQueue.keySet().iterator().next();
    }

    /** Get the execution tree for the queue. Null/blank queueName → default queue. Creates empty tree if absent (for bootstrap). */
    public static QueueExecutionTree getQueueRuntime(String queueName) {
        String q = (queueName != null && !queueName.isBlank()) ? queueName : resolveDefaultQueue();
        return runtimesByQueue.computeIfAbsent(q, k -> new QueueExecutionTree());
    }

    /** Get config for the queue. Use null for default queue (single-queue or env QUEUE_NAME). */
    public static EngineFileConfig getConfig(String queueName) {
        QueueExecutionTree tree = getQueueRuntime(queueName);
        EngineFileConfig c = tree.getConfig();
        if (c == null) {
            throw new IllegalStateException("Engine not bootstrapped for queue '" + (queueName != null ? queueName : resolveDefaultQueue()) + "'. Call WorkerBootstrap.initialize(queueName) first.");
        }
        return c;
    }

    /** Backward compat: config for default queue. */
    public static EngineFileConfig getConfig() {
        return getConfig(null);
    }

    public static void setConfig(String queueName, EngineFileConfig config) {
        String q = (queueName != null && !queueName.isBlank()) ? queueName : "default";
        getQueueRuntime(q).setConfig(config);
    }

    public static CapabilityPlan getCapabilityPlan(String queueName, String pipelineName) {
        QueueExecutionTree tree = getQueueRuntime(queueName);
        Map<String, CapabilityPlan> map = tree.getCapabilityPlansByName();
        if (map == null || map.isEmpty()) {
            throw new IllegalStateException("Engine not bootstrapped for queue '" + (queueName != null ? queueName : resolveDefaultQueue()) + "'. Capability plans not set.");
        }
        String name = pipelineName != null && !pipelineName.isBlank() ? pipelineName : "default";
        CapabilityPlan p = map.get(name);
        if (p == null) {
            throw new IllegalStateException("Unknown pipeline name: '" + name + "' for queue '" + (queueName != null ? queueName : resolveDefaultQueue()) + "'. Available: " + map.keySet());
        }
        return p;
    }

    /** Backward compat: plan for default queue. */
    public static CapabilityPlan getCapabilityPlan(String pipelineName) {
        return getCapabilityPlan(null, pipelineName);
    }

    public static CapabilityPlan getCapabilityPlan() {
        return getCapabilityPlan(null, "default");
    }

    public static void setCapabilityPlans(String queueName, Map<String, CapabilityPlan> plans) {
        String q = (queueName != null && !queueName.isBlank()) ? queueName : "default";
        getQueueRuntime(q).setCapabilityPlans(plans);
    }

    public static CapabilityResolver getCapabilityResolver(String queueName) {
        CapabilityResolver r = getQueueRuntime(queueName).getCapabilityResolver();
        if (r == null) {
            throw new IllegalStateException("Engine not bootstrapped for queue '" + (queueName != null ? queueName : resolveDefaultQueue()) + "'. Capability resolver not set.");
        }
        return r;
    }

    public static CapabilityResolver getCapabilityResolver() {
        return getCapabilityResolver(null);
    }

    public static void setCapabilityResolver(String queueName, CapabilityResolver capabilityResolver) {
        String q = (queueName != null && !queueName.isBlank()) ? queueName : "default";
        getQueueRuntime(q).setCapabilityResolver(capabilityResolver);
    }

    public static OutputContractValidator getOutputContractValidator(String queueName) {
        return getQueueRuntime(queueName).getOutputContractValidator();
    }
    public static OutputContractValidator getOutputContractValidator() { return getOutputContractValidator(null); }
    public static void setOutputContractValidator(String queueName, OutputContractValidator v) {
        String q = (queueName != null && !queueName.isBlank()) ? queueName : "default";
        getQueueRuntime(q).setOutputContractValidator(v);
    }

    public static FeatureFlags getFeatureFlags(String queueName) {
        FeatureFlags f = getQueueRuntime(queueName).getFeatureFlags();
        return f != null ? f : FeatureFlags.fromNames(Collections.emptyList());
    }
    public static FeatureFlags getFeatureFlags() { return getFeatureFlags(null); }
    public static void setFeatureFlags(String queueName, FeatureFlags featureFlags) {
        String q = (queueName != null && !queueName.isBlank()) ? queueName : "default";
        getQueueRuntime(q).setFeatureFlags(featureFlags);
    }

    public static ExecutionPolicyResolver getExecutionPolicyResolver(String queueName) {
        return getQueueRuntime(queueName).getExecutionPolicyResolver();
    }
    public static ExecutionPolicyResolver getExecutionPolicyResolver() { return getExecutionPolicyResolver(null); }
    public static void setExecutionPolicyResolver(String queueName, ExecutionPolicyResolver r) {
        String q = (queueName != null && !queueName.isBlank()) ? queueName : "default";
        getQueueRuntime(q).setExecutionPolicyResolver(r);
    }

    public static BudgetGuardrailEnforcer getBudgetGuardrailEnforcer(String queueName) {
        return getQueueRuntime(queueName).getBudgetGuardrailEnforcer();
    }
    public static BudgetGuardrailEnforcer getBudgetGuardrailEnforcer() { return getBudgetGuardrailEnforcer(null); }
    public static void setBudgetGuardrailEnforcer(String queueName, BudgetGuardrailEnforcer r) {
        String q = (queueName != null && !queueName.isBlank()) ? queueName : "default";
        getQueueRuntime(q).setBudgetGuardrailEnforcer(r);
    }

    public static SecurityHardeningGate getSecurityHardeningGate(String queueName) {
        return getQueueRuntime(queueName).getSecurityHardeningGate();
    }
    public static SecurityHardeningGate getSecurityHardeningGate() { return getSecurityHardeningGate(null); }
    public static void setSecurityHardeningGate(String queueName, SecurityHardeningGate r) {
        String q = (queueName != null && !queueName.isBlank()) ? queueName : "default";
        getQueueRuntime(q).setSecurityHardeningGate(r);
    }

    public static PlanValidator getPlanValidator(String queueName) {
        return getQueueRuntime(queueName).getPlanValidator();
    }
    public static PlanValidator getPlanValidator() { return getPlanValidator(null); }
    public static void setPlanValidator(String queueName, PlanValidator r) {
        String q = (queueName != null && !queueName.isBlank()) ? queueName : "default";
        getQueueRuntime(q).setPlanValidator(r);
    }

    public static FeatureHandlerRegistry getFeatureHandlerRegistry(String queueName) {
        return getQueueRuntime(queueName).getFeatureHandlerRegistry();
    }
    public static FeatureHandlerRegistry getFeatureHandlerRegistry() { return getFeatureHandlerRegistry(null); }
    public static void setFeatureHandlerRegistry(String queueName, FeatureHandlerRegistry r) {
        String q = (queueName != null && !queueName.isBlank()) ? queueName : "default";
        getQueueRuntime(q).setFeatureHandlerRegistry(r);
    }

    public static ExecutionInterceptorChain getExecutionInterceptorChain(String queueName) {
        return getQueueRuntime(queueName).getExecutionInterceptorChain();
    }
    public static ExecutionInterceptorChain getExecutionInterceptorChain() { return getExecutionInterceptorChain(null); }
    public static void setExecutionInterceptorChain(String queueName, ExecutionInterceptorChain r) {
        String q = (queueName != null && !queueName.isBlank()) ? queueName : "default";
        getQueueRuntime(q).setExecutionInterceptorChain(r);
    }

    public static FeatureExecutionPluginRegistry getFeatureExecutionPluginRegistry(String queueName) {
        return getQueueRuntime(queueName).getFeatureExecutionPluginRegistry();
    }
    public static FeatureExecutionPluginRegistry getFeatureExecutionPluginRegistry() { return getFeatureExecutionPluginRegistry(null); }
    public static void setFeatureExecutionPluginRegistry(String queueName, FeatureExecutionPluginRegistry r) {
        String q = (queueName != null && !queueName.isBlank()) ? queueName : "default";
        getQueueRuntime(q).setFeatureExecutionPluginRegistry(r);
    }

    /** Backward compatibility; set by bootstrap. Prefer getConfig(queueName). */
    @Deprecated
    public static EngineFileConfig CONFIG;
}
