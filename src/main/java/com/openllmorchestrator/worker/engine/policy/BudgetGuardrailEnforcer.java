/*
 * Copyright 2026 Open LLM Orchestrator contributors.
 */
package com.openllmorchestrator.worker.engine.policy;

import com.openllmorchestrator.worker.engine.contract.ExecutionContext;

/**
 * Kernel-level enforcement: stop execution when cost, tokens, or iterations exceed caps.
 * When BUDGET_GUARDRAIL is enabled, kernel calls getCaps and checks after each stage.
 */
public interface BudgetGuardrailEnforcer {

    ExecutionCaps getCaps(ExecutionContext context);
    double getCurrentCost(ExecutionContext context);
    int getCurrentTokens(ExecutionContext context);
    int getCurrentIterations(ExecutionContext context);
}

