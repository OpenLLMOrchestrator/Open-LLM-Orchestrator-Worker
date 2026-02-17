package com.openllmorchestrator.worker.plugin.tool;

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

public final class EchoToolPlugin implements CapabilityHandler, ContractCompatibility, PlannerInputDescriptor, PluginTypeDescriptor {

    private static final String CONTRACT_VERSION = "0.0.1";
    public static final String NAME = "com.openllmorchestrator.worker.plugin.tool.EchoToolPlugin";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public CapabilityResult execute(PluginContext context) {
        Map<String, Object> input = context.getOriginalInput();
        String toolName = input != null ? (String) input.get("toolName") : null;
        if (toolName == null || toolName.isBlank()) {
            toolName = "echo";
        }
        Object toolInput = input != null ? input.get("toolInput") : null;
        if (toolInput == null) {
            toolInput = input != null ? input.get("question") : null;
        }
        String result = toolInput != null ? toolInput.toString() : "";
        context.putOutput("toolResult", result);
        context.putOutput("toolName", toolName);
        return CapabilityResult.builder().capabilityName(NAME).data(new HashMap<>(context.getCurrentPluginOutput())).build();
    }

    @Override
    public String getRequiredContractVersion() {
        return CONTRACT_VERSION;
    }

    @Override
    public Set<String> getRequiredInputFieldsForPlanner() {
        return Set.of("toolName", "toolInput", "question");
    }

    @Override
    public String getPlannerDescription() {
        return "Tool: echo tool input as result; for testing or as template.";
    }

    @Override
    public String getPluginType() {
        return PluginTypes.TOOL;
    }
}
