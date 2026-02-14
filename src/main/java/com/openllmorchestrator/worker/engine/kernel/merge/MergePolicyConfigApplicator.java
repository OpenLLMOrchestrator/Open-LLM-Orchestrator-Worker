package com.openllmorchestrator.worker.engine.kernel.merge;

import com.openllmorchestrator.worker.engine.config.EngineFileConfig;

import java.util.Map;

/**
 * Applies merge policy config to the default registry at bootstrap.
 * Config key is policy name (used in asyncOutputMergePolicy); value is either a built-in name
 * (e.g. FIRST_WINS, LAST_WINS, PREFIX_BY_ACTIVITY) or a fully qualified class name.
 */
public final class MergePolicyConfigApplicator {

    private MergePolicyConfigApplicator() {}

    /**
     * Register all entries from config.mergePolicies with the default registry.
     * Value containing a dot is treated as a class name; otherwise as a built-in policy name (alias).
     */
    public static void apply(EngineFileConfig config) {
        if (config == null) return;
        Map<String, String> policies = config.getMergePoliciesEffective();
        if (policies.isEmpty()) return;
        MergePolicyRegistry registry = MergePolicyRegistry.getDefault();
        for (Map.Entry<String, String> e : policies.entrySet()) {
            String name = e.getKey();
            String value = e.getValue();
            if (name == null || name.isBlank() || value == null || value.isBlank()) continue;
            AsyncMergePolicy policy = value.contains(".") ? instantiatePolicy(value) : registry.get(value);
            registry.register(name, policy);
        }
    }

    private static AsyncMergePolicy instantiatePolicy(String className) {
        try {
            Class<?> clazz = Class.forName(className.trim());
            if (!AsyncMergePolicy.class.isAssignableFrom(clazz)) {
                throw new IllegalStateException("Merge policy class must implement AsyncMergePolicy: " + className);
            }
            return (AsyncMergePolicy) clazz.getDeclaredConstructor().newInstance();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to instantiate merge policy: " + className, ex);
        }
    }
}
