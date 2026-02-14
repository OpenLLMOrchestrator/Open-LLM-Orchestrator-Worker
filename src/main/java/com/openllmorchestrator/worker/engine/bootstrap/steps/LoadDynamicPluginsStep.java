package com.openllmorchestrator.worker.engine.bootstrap.steps;

import com.openllmorchestrator.worker.engine.bootstrap.BootstrapContext;
import com.openllmorchestrator.worker.engine.bootstrap.BootstrapStep;
import com.openllmorchestrator.worker.engine.config.EngineFileConfig;
import com.openllmorchestrator.worker.engine.plugin.DynamicPluginLoader;
import com.openllmorchestrator.worker.engine.stage.StageHandler;
import com.openllmorchestrator.worker.engine.stage.activity.ActivityRegistry;
import com.openllmorchestrator.worker.engine.stage.handler.DynamicPluginWrapper;

import java.util.Map;

/**
 * After the base activity registry is built, loads dynamic plugins from JAR paths in config.
 * For each entry in dynamicPlugins (plugin name → JAR path): tries to load the plugin; if the JAR
 * is missing or load fails, registers a {@link DynamicPluginWrapper} that logs and no-ops at runtime.
 */
public final class LoadDynamicPluginsStep implements BootstrapStep {

    @Override
    public void run(BootstrapContext ctx) {
        EngineFileConfig config = ctx.getConfig();
        if (config == null) {
            return;
        }
        Map<String, String> dynamicPlugins = config.getDynamicPluginsEffective();
        if (dynamicPlugins.isEmpty()) {
            return;
        }
        ActivityRegistry base = ctx.getActivityRegistry();
        if (base == null) {
            return;
        }
        ActivityRegistry.Builder builder = ActivityRegistry.builder().registerAll(base.getHandlers());
        for (Map.Entry<String, String> entry : dynamicPlugins.entrySet()) {
            String pluginName = entry.getKey();
            String jarPath = entry.getValue();
            if (pluginName == null || pluginName.isBlank()) {
                continue;
            }
            StageHandler loaded = DynamicPluginLoader.load(jarPath, pluginName);
            StageHandler handler = loaded != null ? loaded : new DynamicPluginWrapper(pluginName, null);
            builder.register(pluginName, handler);
        }
        ctx.setActivityRegistry(builder.build());
    }
}
