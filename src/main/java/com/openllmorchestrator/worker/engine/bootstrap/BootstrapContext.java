package com.openllmorchestrator.worker.engine.bootstrap;

import com.openllmorchestrator.worker.engine.config.EngineFileConfig;
import com.openllmorchestrator.worker.engine.config.env.EnvConfig;
import com.openllmorchestrator.worker.engine.stage.StagePlan;
import com.openllmorchestrator.worker.engine.stage.activity.ActivityRegistry;

import java.util.Map;
import com.openllmorchestrator.worker.engine.stage.custom.CustomStageBucket;
import com.openllmorchestrator.worker.engine.stage.predefined.PredefinedPluginBucket;
import com.openllmorchestrator.worker.engine.stage.resolver.StageResolver;

import lombok.Getter;
import lombok.Setter;

/** Mutable context passed through bootstrap steps. */
@Getter
@Setter
public class BootstrapContext {
    private EnvConfig envConfig;
    private EngineFileConfig config;
    private PredefinedPluginBucket predefinedBucket;
    private CustomStageBucket customBucket;
    private ActivityRegistry activityRegistry;
    private StageResolver resolver;
    /** Single plan (legacy); used when plans map is not set. */
    private StagePlan plan;
    /** Named pipeline plans: pipeline name → StagePlan. When set, use getStagePlan(name) at runtime. */
    private Map<String, StagePlan> plans;
}
