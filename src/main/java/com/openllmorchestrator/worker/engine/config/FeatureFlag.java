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
package com.openllmorchestrator.worker.engine.config;

/**
 * Feature flags. Only enabled features execute; disabled features run no code paths to achieve performance.
 * Configure in config file (enabledFeatures list) and load at bootstrap.
 * Design: (1) Eliminate execution hierarchy for disabled features during bootstrap where possible.
 * (2) If a runtime decision is needed, use a minimal enum check at root level to eliminate the feature.
 */
public enum FeatureFlag {

    /** Human-in-the-loop: suspend workflow, receive signal, resume with payload. */
    HUMAN_SIGNAL,
    /** Streaming stage API: StreamObserver, token/intermediate updates. */
    STREAMING,
    /** Durable agent identity: AgentContext, memory store. */
    AGENT_CONTEXT,
    /** Determinism policy: freeze model params, persist tool/retrieval outputs, randomness seed. */
    DETERMINISM_POLICY,
    /** Checkpointable stages: resumeFrom, branchFrom. */
    CHECKPOINTABLE_STAGE,
    /** Output contract: schema validation, enforceStrict. */
    OUTPUT_CONTRACT,
    /** Execution graph (DAG): topological order; when off, linear stageOrder only. */
    EXECUTION_GRAPH,
    /** Stage result envelope: CapabilityMetadata, DependencyRef, deterministic flag on result. */
    STAGE_RESULT_ENVELOPE,
    /** Versioned state: stepId, executionId, immutable state per step. */
    VERSIONED_STATE,
    /** Interceptor layer: beforeStage, afterStage, onError. */
    INTERCEPTORS,
    /** Planner + Plan executor: dynamic plan in context, kernel runs sub-plan. */
    PLANNER_PLAN_EXECUTOR,
    /** Execution snapshot: snapshot(), ContextSnapshot for observability/recovery. */
    EXECUTION_SNAPSHOT,
    /** Unified policy engine: ExecutionPolicyResolver (model by tenant tier, tool whitelist, budget/iteration/token caps). */
    POLICY_ENGINE,
    /** Kernel-level budget/cost guardrail: stop execution when cost/tokens/iterations exceed caps. */
    BUDGET_GUARDRAIL,
    /** Concurrency isolation: queue-per-stage / queue-per-tenant topology for scaling. */
    CONCURRENCY_ISOLATION,
    /** Security hardening: prompt injection defense, tool allowlist, output scanning, context poisoning detection. */
    SECURITY_HARDENING,
    /** Plan safety validation: allowed stages, max depth, no cycles, no unauthorized injection before running dynamic plan. */
    PLAN_SAFETY_VALIDATION,
    /** Visual execution graph export: DOT, Mermaid, JSON for UI. */
    EXECUTION_GRAPH_EXPORT,
}

