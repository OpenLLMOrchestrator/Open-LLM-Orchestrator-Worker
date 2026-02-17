package com.openllmorchestrator.worker.plugin.access;

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

public final class AllowAllAccessControlPlugin implements CapabilityHandler, ContractCompatibility, PlannerInputDescriptor, PluginTypeDescriptor {
    private static final String CONTRACT_VERSION = "0.0.1";
    public static final String NAME = "com.openllmorchestrator.worker.plugin.access.AllowAllAccessControlPlugin";

    @Override
    public String name() { return NAME; }

    @Override
    public CapabilityResult execute(PluginContext context) {
        Map<String, Object> input = context.getOriginalInput();
        Object allowKey = input != null ? input.get("allowKey") : null;
        if (allowKey != null && !Boolean.TRUE.equals(allowKey) && !"true".equalsIgnoreCase(String.valueOf(allowKey))) {
            context.putOutput("accessDenied", true);
            context.putOutput("reason", "allowKey not set or not true");
        } else {
            context.putOutput("accessAllowed", true);
        }
        return CapabilityResult.builder().capabilityName(NAME).data(new HashMap<>(context.getCurrentPluginOutput())).build();
    }

    @Override
    public String getRequiredContractVersion() { return CONTRACT_VERSION; }

    @Override
    public Set<String> getRequiredInputFieldsForPlanner() { return Set.of("allowKey"); }

    @Override
    public String getPlannerDescription() { return "Access control: allow-all; optional allowKey check."; }

    @Override
    public String getPluginType() { return PluginTypes.ACCESS_CONTROL; }
}
