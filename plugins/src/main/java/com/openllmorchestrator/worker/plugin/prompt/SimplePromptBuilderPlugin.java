package com.openllmorchestrator.worker.plugin.prompt;

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

public final class SimplePromptBuilderPlugin implements CapabilityHandler, ContractCompatibility, PlannerInputDescriptor, PluginTypeDescriptor {
    private static final String CONTRACT_VERSION = "0.0.1";
    public static final String NAME = "com.openllmorchestrator.worker.plugin.prompt.SimplePromptBuilderPlugin";

    @Override
    public String name() { return NAME; }

    @Override
    public CapabilityResult execute(PluginContext context) {
        Map<String, Object> input = context.getOriginalInput();
        Map<String, Object> accumulated = context.getAccumulatedOutput();
        String question = input != null ? (String) input.get("question") : null;
        if (question == null && accumulated != null) question = String.valueOf(accumulated.get("question"));
        if (question == null) question = "";
        Object ctxObj = accumulated != null ? accumulated.get("retrievedChunks") : (accumulated != null ? accumulated.get("context") : null);
        String contextStr = ctxObj != null ? ctxObj.toString() : "";
        String template = input != null ? (String) input.get("template") : null;
        if (template == null || template.isBlank()) template = "Question: {question}\n\nContext:\n{context}";
        String resultVal = accumulated != null && accumulated.get("result") != null ? String.valueOf(accumulated.get("result")) : "";
        String built = template.replace("{question}", question).replace("{context}", contextStr).replace("{result}", resultVal);
        context.putOutput("builtPrompt", built);
        return CapabilityResult.builder().capabilityName(NAME).data(new HashMap<>(context.getCurrentPluginOutput())).build();
    }

    @Override
    public String getRequiredContractVersion() { return CONTRACT_VERSION; }

    @Override
    public Set<String> getRequiredInputFieldsForPlanner() { return Set.of("question", "context", "template", "retrievedChunks"); }

    @Override
    public String getPlannerDescription() { return "Prompt builder: template with question, context, result placeholders."; }

    @Override
    public String getPluginType() { return PluginTypes.PROMPT_BUILDER; }
}
