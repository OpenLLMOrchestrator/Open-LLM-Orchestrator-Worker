package com.openllmorchestrator.worker.engine.plugin;

import com.openllmorchestrator.worker.engine.stage.StageHandler;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * Loads a StageHandler from a JAR file. The JAR must provide a service implementation via
 * META-INF/services/com.openllmorchestrator.worker.engine.stage.StageHandler.
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
                    log.info("Dynamic plugin '{}': no StageHandler service found in JAR '{}' (expect META-INF/services/com.openllmorchestrator.worker.engine.stage.StageHandler); skipping.", pluginName, resolved);
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
}
