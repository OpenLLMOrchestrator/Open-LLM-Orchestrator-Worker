package com.openllmorchestrator.worker.plugin.caching;

import com.openllmorchestrator.worker.contract.ContractCompatibility;
import com.openllmorchestrator.worker.contract.PluginContext;
import com.openllmorchestrator.worker.contract.PlannerInputDescriptor;
import com.openllmorchestrator.worker.contract.PluginTypeDescriptor;
import com.openllmorchestrator.worker.contract.PluginTypes;
import com.openllmorchestrator.worker.contract.StageHandler;
import com.openllmorchestrator.worker.contract.StageResult;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryCachingPlugin implements StageHandler, ContractCompatibility, PlannerInputDescriptor, PluginTypeDescriptor {
    private static final String CONTRACT_VERSION = "0.0.1";
    public static final String NAME = "com.openllmorchestrator.worker.plugin.caching.InMemoryCachingPlugin";
    private static final String STATE_PREFIX = "cache:";
    private static final Map<String, Object> STATIC_CACHE = new ConcurrentHashMap<>();

    @Override
    public String name() { return NAME; }

    @Override
    public StageResult execute(PluginContext context) {
        Map<String, Object> input = context.getOriginalInput();
        String key = input != null ? (String) input.get("cacheKey") : null;
        Object valueToStore = input != null ? input.get("value") : null;
        boolean hit = false;
        Object cached = null;
        if (key != null && !key.isBlank()) {
            String stateKey = STATE_PREFIX + key;
            if (valueToStore != null) {
                context.put(stateKey, valueToStore);
                STATIC_CACHE.put(key, valueToStore);
                context.putOutput("cachedValue", valueToStore);
            } else {
                cached = context.get(stateKey);
                if (cached == null) cached = STATIC_CACHE.get(key);
                hit = cached != null;
                context.putOutput("cachedValue", cached);
            }
        }
        context.putOutput("cacheHit", hit);
        return StageResult.builder().stageName(NAME).data(new HashMap<>(context.getCurrentPluginOutput())).build();
    }

    @Override
    public String getRequiredContractVersion() { return CONTRACT_VERSION; }

    @Override
    public Set<String> getRequiredInputFieldsForPlanner() { return Set.of("cacheKey", "value"); }

    @Override
    public String getPlannerDescription() { return "Caching: in-memory get/set by cacheKey; outputs cachedValue, cacheHit."; }

    @Override
    public String getPluginType() { return PluginTypes.CACHING; }
}
