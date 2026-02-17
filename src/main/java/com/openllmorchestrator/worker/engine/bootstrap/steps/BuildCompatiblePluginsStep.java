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
package com.openllmorchestrator.worker.engine.bootstrap.steps;

import com.openllmorchestrator.worker.contract.ContractVersion;
import com.openllmorchestrator.worker.contract.ContractVersionException;
import com.openllmorchestrator.worker.contract.CapabilityHandler;
import com.openllmorchestrator.worker.engine.bootstrap.BootstrapContext;
import com.openllmorchestrator.worker.engine.bootstrap.BootstrapStep;
import com.openllmorchestrator.worker.engine.config.EngineFileConfig;
import com.openllmorchestrator.worker.engine.capability.activity.ActivityRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * After the activity registry (built-in + dynamic plugins) is built, checks contract compatibility
 * for each plugin and optionally filters by config's {@code plugins} allow-list. Builds a
 * compatible-activity registry used for static pipeline structure and dynamic resolution (PLANNER, PLAN_EXECUTOR).
 */
public final class BuildCompatiblePluginsStep implements BootstrapStep {

    private static final Logger log = LoggerFactory.getLogger(BuildCompatiblePluginsStep.class);

    @Override
    public void run(BootstrapContext ctx) {
        EngineFileConfig config = ctx.getConfig();
        ActivityRegistry full = ctx.getActivityRegistry();
        if (full == null) {
            ctx.setCompatibleActivityRegistry(null);
            return;
        }
        Set<String> allowed = config != null ? config.getPluginsEffective() : null;
        Map<String, CapabilityHandler> compatible = new LinkedHashMap<>();
        for (Map.Entry<String, CapabilityHandler> e : full.getHandlers().entrySet()) {
            String name = e.getKey();
            CapabilityHandler handler = e.getValue();
            if (allowed != null && !allowed.contains(name)) {
                continue;
            }
            try {
                ContractVersion.requireCompatible(handler);
                compatible.put(name, handler);
            } catch (ContractVersionException ex) {
                if (allowed != null) {
                    throw new IllegalStateException("Plugin not compatible: " + name + " (" + ex.getMessage() + ")", ex);
                }
                log.warn("Skipping incompatible plugin {}: {}", name, ex.getMessage());
            }
        }
        if (allowed != null) {
            for (String name : allowed) {
                if (!compatible.containsKey(name)) {
                    throw new IllegalStateException(
                            "Plugin in config.plugins not found or failed compatibility: " + name
                                    + ". Ensure it is registered (built-in or dynamicPlugins) and contract-compatible.");
                }
            }
        }
        ctx.setCompatibleActivityRegistry(
                compatible.isEmpty() ? ActivityRegistry.builder().build() : ActivityRegistry.builder().registerAll(compatible).build());
    }
}
