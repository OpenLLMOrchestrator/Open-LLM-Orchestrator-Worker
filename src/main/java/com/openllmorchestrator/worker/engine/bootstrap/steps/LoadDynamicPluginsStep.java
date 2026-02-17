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

import com.openllmorchestrator.worker.contract.CapabilityHandler;
import com.openllmorchestrator.worker.engine.bootstrap.BootstrapContext;
import com.openllmorchestrator.worker.engine.bootstrap.BootstrapStep;
import com.openllmorchestrator.worker.engine.config.EngineFileConfig;
import com.openllmorchestrator.worker.engine.plugin.DynamicPluginLoader;
import com.openllmorchestrator.worker.engine.capability.activity.ActivityRegistry;
import com.openllmorchestrator.worker.engine.capability.handler.DynamicPluginWrapper;

import java.util.Map;
import java.util.ServiceLoader;

/**
 * After the base activity registry is built, loads plugins from:
 * (1) Classpath (compile-time plugins): all {@link CapabilityHandler} implementations via ServiceLoader.
 *    Registered by name(), by class name (FQCN), and by worker-package alias when config sets pluginRepoPackagePrefix.
 * (2) Config dynamicPluginJars (list of JAR paths, loadAll each) for additional runtime JARs.
 * (3) Config dynamicPlugins (plugin name → JAR path), for explicit name→path mapping.
 * If a JAR is missing or load fails, registers a {@link DynamicPluginWrapper} that logs and no-ops at runtime.
 */
public final class LoadDynamicPluginsStep implements BootstrapStep {

    private static final String WORKER_PLUGIN_PACKAGE_PREFIX = "com.openllmorchestrator.worker.plugin.";

    @Override
    public void run(BootstrapContext ctx) {
        ActivityRegistry base = ctx.getActivityRegistry();
        if (base == null) {
            return;
        }
        ActivityRegistry.Builder builder = ActivityRegistry.builder().registerAll(base.getHandlers());
        EngineFileConfig config = ctx.getConfig();
        String pluginRepoPrefix = config != null ? config.getPluginRepoPackagePrefix() : null;

        // (1) Compile-time plugins: discover from classpath (same worker JAR / fat JAR)
        ServiceLoader.load(CapabilityHandler.class).forEach(handler ->
                registerHandler(builder, handler, pluginRepoPrefix));

        if (config != null) {
            for (String jarPath : config.getDynamicPluginJarsEffective()) {
                if (jarPath == null || jarPath.isBlank()) continue;
                Map<String, CapabilityHandler> loaded = DynamicPluginLoader.loadAll(jarPath);
                loaded.forEach((name, handler) -> registerHandler(builder, handler, pluginRepoPrefix));
            }
        }

        if (config != null) {
            for (Map.Entry<String, String> entry : config.getDynamicPluginsEffective().entrySet()) {
                String pluginName = entry.getKey();
                String jarPath = entry.getValue();
                if (pluginName == null || pluginName.isBlank()) continue;
                CapabilityHandler loaded = DynamicPluginLoader.load(jarPath, pluginName);
                CapabilityHandler handler = loaded != null ? loaded : new DynamicPluginWrapper(pluginName, null);
                builder.register(pluginName, handler);
                registerHandler(builder, handler, pluginRepoPrefix);
            }
        }

        ctx.setActivityRegistry(builder.build());
    }

    private static void registerHandler(ActivityRegistry.Builder builder, CapabilityHandler handler, String pluginRepoPackagePrefix) {
        if (handler == null) return;
        String name = handler.name();
        if (name != null && !name.isBlank()) {
            builder.register(name, handler);
        }
        String className = handler.getClass().getName();
        if (className != null && !className.equals(name)) {
            builder.register(className, handler);
        }
        if (pluginRepoPackagePrefix != null && !pluginRepoPackagePrefix.isBlank() && className != null && className.startsWith(pluginRepoPackagePrefix)) {
            String suffix = className.substring(pluginRepoPackagePrefix.length());
            if (suffix.startsWith(".")) suffix = suffix.substring(1);
            String workerAlias = WORKER_PLUGIN_PACKAGE_PREFIX + suffix;
            builder.register(workerAlias, handler);
        }
    }
}

