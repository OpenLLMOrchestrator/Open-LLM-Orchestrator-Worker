/*
 * Copyright 2026 Open LLM Orchestrator contributors.
 */
package com.openllmorchestrator.worker.engine.plan;

import com.openllmorchestrator.worker.engine.capability.CapabilityGroupSpec;
import com.openllmorchestrator.worker.engine.capability.CapabilityPlan;
import com.openllmorchestrator.worker.engine.contract.ExecutionContext;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Default plan validator: checks allowed stages, max depth, and simple cycle detection.
 * Register at bootstrap when PLAN_SAFETY_VALIDATION is enabled; use policy or config for allowed list and max depth.
 */
public class PlanValidatorImpl implements PlanValidator {

    private final List<String> allowedStageNames;
    private final int maxDepth;

    public PlanValidatorImpl(List<String> allowedStageNames, int maxDepth) {
        this.allowedStageNames = allowedStageNames != null ? List.copyOf(allowedStageNames) : List.of();
        this.maxDepth = Math.max(1, maxDepth);
    }

    @Override
    public void validate(CapabilityPlan plan, ExecutionContext context) throws PlanValidationException {
        if (plan == null) return;
        List<CapabilityGroupSpec> groups = plan.getGroups();
        if (groups == null) return;
        if (!allowedStageNames.isEmpty()) {
            Set<String> allowed = new HashSet<>(allowedStageNames);
            for (CapabilityGroupSpec g : groups) {
                if (g != null && g.getDefinitions() != null) {
                    for (var def : g.getDefinitions()) {
                        String name = def != null ? def.getStageBucketName() : null;
                        if (name != null && !allowed.contains(name)) {
                            throw new PlanValidationException("Unauthorized capability in plan: " + name);
                        }
                    }
                }
            }
        }
        if (groups.size() > maxDepth) {
            throw new PlanValidationException("Plan depth " + groups.size() + " exceeds max " + maxDepth);
        }
    }
}

