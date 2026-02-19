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
package com.openllmorchestrator.worker.engine.kernel.feature;

import com.openllmorchestrator.worker.contract.FeatureExecutionPlugin;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * Registry of feature execution plugins by name. Built at bootstrap via ServiceLoader
 * so config can reference plugins by name in featurePlugins (global section).
 */
public final class FeatureExecutionPluginRegistry {

    private final Map<String, FeatureExecutionPlugin> byName;

    public FeatureExecutionPluginRegistry(Map<String, FeatureExecutionPlugin> byName) {
        this.byName = byName != null ? Collections.unmodifiableMap(new LinkedHashMap<>(byName)) : Collections.emptyMap();
    }

    public FeatureExecutionPlugin get(String pluginName) {
        return pluginName != null ? byName.get(pluginName) : null;
    }

    /** Load from classpath via ServiceLoader and register by {@link FeatureExecutionPlugin#name()}. */
    public static FeatureExecutionPluginRegistry loadFromServiceLoader() {
        Map<String, FeatureExecutionPlugin> map = new LinkedHashMap<>();
        for (FeatureExecutionPlugin plugin : ServiceLoader.load(FeatureExecutionPlugin.class)) {
            String name = plugin.name();
            if (name != null && !name.isBlank()) {
                map.putIfAbsent(name, plugin);
            }
        }
        return new FeatureExecutionPluginRegistry(map);
    }
}
