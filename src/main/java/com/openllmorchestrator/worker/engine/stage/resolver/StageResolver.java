package com.openllmorchestrator.worker.engine.stage.resolver;

import com.openllmorchestrator.worker.engine.config.EngineFileConfig;
import com.openllmorchestrator.worker.engine.stage.StageHandler;
import com.openllmorchestrator.worker.engine.stage.activity.ActivityRegistry;
import com.openllmorchestrator.worker.engine.stage.custom.CustomStageBucket;
import com.openllmorchestrator.worker.engine.stage.predefined.PredefinedPluginBucket;
import com.openllmorchestrator.worker.engine.stage.predefined.PredefinedStages;

import java.util.Map;

/** Resolves stage/activity name to handler: predefined stage, then activity (plugin) name, then custom. */
public final class StageResolver {
    private static final String DEFAULT_PLUGIN_ID = "default";

    private final EngineFileConfig config;
    private final PredefinedPluginBucket predefinedBucket;
    private final CustomStageBucket customBucket;
    private final ActivityRegistry activityRegistry;

    public StageResolver(EngineFileConfig config,
                         PredefinedPluginBucket predefinedBucket,
                         CustomStageBucket customBucket) {
        this(config, predefinedBucket, customBucket, null);
    }

    public StageResolver(EngineFileConfig config,
                         PredefinedPluginBucket predefinedBucket,
                         CustomStageBucket customBucket,
                         ActivityRegistry activityRegistry) {
        this.config = config;
        this.predefinedBucket = predefinedBucket;
        this.customBucket = customBucket;
        this.activityRegistry = activityRegistry;
    }

    /**
     * Resolve a name to a handler. Order: predefined stage (by stagePlugins) → activity/plugin name → custom bucket.
     */
    public StageHandler resolve(String stageName) {
        if (stageName == null || stageName.isBlank()) {
            return null;
        }
        if (PredefinedStages.isPredefined(stageName)) {
            String pluginId = getPluginIdForPredefined(stageName);
            StageHandler h = predefinedBucket.get(stageName, pluginId);
            if (h != null) return h;
        }
        if (activityRegistry != null) {
            StageHandler h = activityRegistry.get(stageName);
            if (h != null) return h;
        }
        return customBucket.get(stageName);
    }

    public boolean canResolve(String stageName) {
        return resolve(stageName) != null;
    }

    private String getPluginIdForPredefined(String stageName) {
        if (config == null || config.getPipeline() == null) {
            return DEFAULT_PLUGIN_ID;
        }
        Map<String, String> stagePlugins = config.getPipeline().getStagePlugins();
        if (stagePlugins == null) {
            return DEFAULT_PLUGIN_ID;
        }
        String pluginId = stagePlugins.get(stageName);
        return pluginId == null || pluginId.isBlank() ? DEFAULT_PLUGIN_ID : pluginId;
    }
}
