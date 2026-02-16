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
package com.openllmorchestrator.worker.engine.plugin;

import com.openllmorchestrator.worker.contract.StageHandler;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * Loads a StageHandler from a JAR file. The JAR must provide a service implementation via
 * META-INF/services/com.openllmorchestrator.worker.contract.StageHandler.
 * If the file does not exist or loading fails, returns null (caller should log and register a no-op wrapper).
 */
@Slf4j
public final class DynamicPluginLoader {

    private DynamicPluginLoader() {}

    /**
     * Try to load a single StageHandler from the given JAR path.
     *
     * @param jarPath path to the JAR (absolute or relative to current working directory)
     * @param pluginName name used for logging and registration
     * @return the loaded handler, or null if the file is missing, not a JAR, or no StageHandler service is found
     */
    public static StageHandler load(String jarPath, String pluginName) {
        if (jarPath == null || jarPath.isBlank()) {
            log.debug("Dynamic plugin '{}': JAR path is blank, skipping.", pluginName);
            return null;
        }
        Path resolved = Paths.get(jarPath).normalize();
        File file = resolved.toFile();
        if (!file.exists()) {
            log.info("Dynamic plugin '{}' not available at path '{}' (file does not exist); skipping load.", pluginName, resolved);
            return null;
        }
        if (!file.isFile()) {
            log.warn("Dynamic plugin '{}': path '{}' is not a file; skipping load.", pluginName, resolved);
            return null;
        }
        if (!file.getName().toLowerCase().endsWith(".jar")) {
            log.warn("Dynamic plugin '{}': path '{}' does not appear to be a JAR; skipping load.", pluginName, resolved);
            return null;
        }
        try {
            URL jarUrl = file.toURI().toURL();
            try (URLClassLoader loader = new URLClassLoader(new URL[]{jarUrl}, Thread.currentThread().getContextClassLoader())) {
                ServiceLoader<StageHandler> serviceLoader = ServiceLoader.load(StageHandler.class, loader);
                Iterator<StageHandler> it = serviceLoader.iterator();
                if (!it.hasNext()) {
                    log.info("Dynamic plugin '{}': no StageHandler service found in JAR '{}' (expect META-INF/services/com.openllmorchestrator.worker.contract.StageHandler); skipping.", pluginName, resolved);
                    return null;
                }
                StageHandler handler = it.next();
                if (it.hasNext()) {
                    log.warn("Dynamic plugin '{}': multiple StageHandler implementations in JAR '{}'; using first.", pluginName, resolved);
                }
                log.info("Dynamic plugin '{}' loaded successfully from '{}'.", pluginName, resolved);
                return handler;
            }
        } catch (Exception e) {
            log.warn("Dynamic plugin '{}' failed to load from '{}': {}; skipping.", pluginName, resolved, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Load all StageHandler implementations from the given JAR. Each handler is registered by its {@link StageHandler#name()}.
     * Use when one JAR provides multiple plugins (e.g. sample-plugins.jar).
     *
     * @param jarPath path to the JAR (absolute or relative to current working directory)
     * @return map of plugin name to handler; empty if file missing, not a JAR, or no services found; never null
     */
    public static Map<String, StageHandler> loadAll(String jarPath) {
        Map<String, StageHandler> out = new LinkedHashMap<>();
        if (jarPath == null || jarPath.isBlank()) {
            return out;
        }
        Path resolved = Paths.get(jarPath).normalize();
        File file = resolved.toFile();
        if (!file.exists() || !file.isFile() || !file.getName().toLowerCase().endsWith(".jar")) {
            log.debug("Dynamic plugin JAR '{}' not available or not a JAR; skipping loadAll.", resolved);
            return out;
        }
        try {
            URL jarUrl = file.toURI().toURL();
            try (URLClassLoader loader = new URLClassLoader(new URL[]{jarUrl}, Thread.currentThread().getContextClassLoader())) {
                ServiceLoader<StageHandler> serviceLoader = ServiceLoader.load(StageHandler.class, loader);
                for (StageHandler handler : serviceLoader) {
                    String name = handler != null ? handler.name() : null;
                    if (name != null && !name.isBlank()) {
                        out.put(name, handler);
                        log.info("Dynamic plugin '{}' loaded from JAR '{}'.", name, resolved);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Dynamic plugin JAR '{}' failed to load: {}; skipping.", resolved, e.getMessage(), e);
        }
        return out;
    }
}

