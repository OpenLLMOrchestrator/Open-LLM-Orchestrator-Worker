package com.openllmorchestrator.worker.plugin.observability;

import com.openllmorchestrator.worker.contract.ContractCompatibility;
import com.openllmorchestrator.worker.contract.PluginContext;
import com.openllmorchestrator.worker.contract.PlannerInputDescriptor;
import com.openllmorchestrator.worker.contract.PluginTypeDescriptor;
import com.openllmorchestrator.worker.contract.PluginTypes;
import com.openllmorchestrator.worker.contract.CapabilityHandler;
import com.openllmorchestrator.worker.contract.CapabilityResult;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * OBSERVABILITY: pass-through; forwards accumulated output and optionally records metrics placeholder.
 * Output: observed (true), and copies through key fields (e.g. result, question) for downstream.
 */
public final class PassThroughObservabilityPlugin implements CapabilityHandler, ContractCompatibility, PlannerInputDescriptor, PluginTypeDescriptor {
    private static final String CONTRACT_VERSION = "0.0.1";
    public static final String NAME = "com.openllmorchestrator.worker.plugin.observability.PassThroughObservabilityPlugin";

    @Override
    public String name() { return NAME; }

    @Override
    public CapabilityResult execute(PluginContext context) {
        Map<String, Object> accumulated = context.getAccumulatedOutput();
        context.putOutput("observed", true);
        if (accumulated != null) {
            if (accumulated.containsKey("result")) context.putOutput("result", accumulated.get("result"));
            if (accumulated.containsKey("question")) context.putOutput("question", accumulated.get("question"));
        }
        return CapabilityResult.builder().capabilityName(NAME).data(new HashMap<>(context.getCurrentPluginOutput())).build();
    }

    @Override
    public String getRequiredContractVersion() { return CONTRACT_VERSION; }

    @Override
    public Set<String> getRequiredInputFieldsForPlanner() { return Set.of("result", "question"); }

    @Override
    public String getPlannerDescription() { return "Observability: pass-through with observed flag; for metrics/tracing hooks."; }

    @Override
    public String getPluginType() { return PluginTypes.OBSERVABILITY; }
}
