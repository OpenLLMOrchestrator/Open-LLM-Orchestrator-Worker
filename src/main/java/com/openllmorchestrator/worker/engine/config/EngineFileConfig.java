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
package com.openllmorchestrator.worker.engine.config;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.openllmorchestrator.worker.engine.contract.ExecutionGraph;
import com.openllmorchestrator.worker.engine.config.activity.ActivityDefaultsConfig;
import com.openllmorchestrator.worker.engine.config.database.DatabaseConfig;
import com.openllmorchestrator.worker.engine.config.env.EnvConfig;
import com.openllmorchestrator.worker.engine.config.pipeline.CapabilityDef;
import com.openllmorchestrator.worker.engine.config.pipeline.PipelineSection;
import com.openllmorchestrator.worker.engine.config.queue.QueueTopologyConfig;
import com.openllmorchestrator.worker.engine.config.redis.RedisConfig;
import com.openllmorchestrator.worker.engine.config.temporal.TemporalConfig;
import com.openllmorchestrator.worker.engine.config.worker.WorkerConfig;
import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Root engine config. One package per section (OCP). Nothing hardcoded in engine. */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class EngineFileConfig {
    private String configVersion;
    private WorkerConfig worker;
    private TemporalConfig temporal;
    private ActivityDefaultsConfig activity;
    private RedisConfig redis;
    private DatabaseConfig database;
    /**
     * Capability order for execution. Fixed flow for predefined capabilities; only capabilities present in pipeline root are run, in this order.
     * Accepts JSON key "capabilityOrder" (preferred) or "stageOrder" (backward compatibility) via {@link com.fasterxml.jackson.annotation.JsonAlias}.
     */
    @JsonAlias("stageOrder")
    private List<String> capabilityOrder;
    /**
     * User-defined capabilities: capability name → definition (pluginType + name).
     * These can be referenced anywhere in the capability flow (root or inside groups).
     * Predefined capabilities (ACCESS, MODEL, etc.) need not be listed here.
     */
    private Map<String, CapabilityDef> capabilities;
    /**
     * Capability name → plugin id for predefined capabilities (e.g. ACCESS → "default").
     * Used when resolving handlers. Pipeline-level capabilityPlugins override when set.
     */
    @JsonAlias("stagePlugins")
    private Map<String, String> capabilityPlugins;
    /**
     * Async merge policy name → implementation. Value is either a built-in name (FIRST_WINS, LAST_WINS, PREFIX_BY_ACTIVITY)
     * or a fully qualified class name implementing AsyncMergePolicy. Registered at bootstrap; referenced in pipeline/group asyncOutputMergePolicy.
     */
    private Map<String, String> mergePolicies;
    /** Named pipelines: user-defined name → pipeline config (e.g. "chat", "document-extraction", "default"). At least one required. */
    private Map<String, PipelineSection> pipelines;
    /**
     * Dynamic plugins: plugin name (activity id) → path to JAR file.
     * At bootstrap the engine tries to load each JAR and register a CapabilityHandler; if the file is missing or load fails, a no-op wrapper is registered and a log message is emitted. At runtime, if the plugin was not loaded, the wrapper logs and returns empty output.
     */
    private Map<String, String> dynamicPlugins;
    /**
     * JAR paths that provide multiple plugins via ServiceLoader. Each JAR is loaded with {@link com.openllmorchestrator.worker.engine.plugin.DynamicPluginLoader#loadAll(String)} and every CapabilityHandler is registered by its {@link com.openllmorchestrator.worker.contract.CapabilityHandler#name()}.
     */
    private List<String> dynamicPluginJars;

    /**
     * Allowed plugins for static pipelines and dynamic use (PLANNER, PLAN_EXECUTOR).
     * List of plugin names (FQCN or activity id) that may be used. At bootstrap the engine checks each for
     * contract compatibility and builds a compatible map; only these plugins are available for static pipeline
     * structure and for dynamic resolution. If null or empty, all registered plugins that pass compatibility are allowed.
     */
    private List<String> plugins;

    /**
     * Enabled feature flags by name (e.g. HUMAN_SIGNAL, STREAMING, AGENT_CONTEXT).
     * Only these features execute; disabled features run no code. Loaded at bootstrap.
     */
    private List<String> enabledFeatures;

    /** Queue topology for concurrency isolation (queue-per-stage, queue-per-tenant). When CONCURRENCY_ISOLATION enabled. */
    private QueueTopologyConfig queueTopology;

    /** Merge: connection config (queue, redis, db) from env; server config from storage. */
    public static EngineFileConfig mergeFromEnv(EnvConfig env, EngineFileConfig fromStorage) {
        EngineFileConfig merged = new EngineFileConfig();
        merged.configVersion = fromStorage != null ? fromStorage.configVersion : "1.0";
        merged.worker = env.getWorker();
        merged.redis = env.getRedis();
        merged.database = env.getDatabase();
        merged.temporal = fromStorage != null ? fromStorage.temporal : null;
        merged.activity = fromStorage != null ? fromStorage.activity : null;
        merged.pipelines = fromStorage != null ? fromStorage.pipelines : null;
        merged.capabilityOrder = fromStorage != null ? fromStorage.capabilityOrder : null;
        merged.capabilities = fromStorage != null ? fromStorage.capabilities : null;
        merged.capabilityPlugins = fromStorage != null ? fromStorage.capabilityPlugins : null;
        merged.mergePolicies = fromStorage != null ? fromStorage.mergePolicies : null;
        merged.dynamicPlugins = fromStorage != null ? fromStorage.dynamicPlugins : null;
        merged.dynamicPluginJars = fromStorage != null ? fromStorage.dynamicPluginJars : null;
        merged.plugins = fromStorage != null ? fromStorage.plugins : null;
        merged.enabledFeatures = fromStorage != null ? fromStorage.enabledFeatures : null;
        merged.queueTopology = fromStorage != null ? fromStorage.queueTopology : null;
        return merged;
    }

    /** Effective feature flags from config; when null/empty, no optional features are enabled. */
    public com.openllmorchestrator.worker.engine.config.FeatureFlags getFeatureFlagsEffective() {
        return com.openllmorchestrator.worker.engine.config.FeatureFlags.fromNames(
                enabledFeatures != null ? enabledFeatures : List.of());
    }

    /** Effective merge policies from config (may be null/empty). */
    public Map<String, String> getMergePoliciesEffective() {
        return mergePolicies != null ? mergePolicies : Collections.emptyMap();
    }

    /** Effective capability order: config capabilityOrder (or stageOrder in JSON) if set, else predefined order in code. */
    public List<String> getCapabilityOrderEffective() {
        if (capabilityOrder != null && !capabilityOrder.isEmpty()) {
            return capabilityOrder;
        }
        return com.openllmorchestrator.worker.engine.capability.predefined.PredefinedCapabilities.orderedNames();
    }

    /**
     * Effective execution graph. When capabilityOrder is set, a linear graph is built (backward compatibility).
     * For future DAG support, config can provide an explicit graph; until then, graph is derived from capabilityOrder.
     */
    public ExecutionGraph getExecutionGraphEffective() {
        return ExecutionGraph.fromLinearOrder(getCapabilityOrderEffective());
    }

    /** Effective capability plugins: engine-level if set. */
    public Map<String, String> getCapabilityPluginsEffective() {
        return capabilityPlugins != null && !capabilityPlugins.isEmpty() ? capabilityPlugins : Collections.emptyMap();
    }

    /** User-defined capabilities (name → definition). Empty if not set. Used to resolve custom capability names anywhere in the flow. */
    public Map<String, CapabilityDef> getCapabilitiesEffective() {
        return capabilities != null && !capabilities.isEmpty() ? capabilities : Collections.emptyMap();
    }

    /** All pipeline names to use at runtime. Requires at least one pipeline in config. */
    public Map<String, PipelineSection> getPipelinesEffective() {
        return pipelines != null ? pipelines : Collections.emptyMap();
    }

    /** Effective dynamic plugin name → JAR path. */
    public Map<String, String> getDynamicPluginsEffective() {
        return dynamicPlugins != null ? dynamicPlugins : Collections.emptyMap();
    }

    /** Effective list of JAR paths that provide multiple plugins (loadAll). */
    public List<String> getDynamicPluginJarsEffective() {
        return dynamicPluginJars != null ? dynamicPluginJars : Collections.emptyList();
    }

    /** Allowed plugin names for static/dynamic use. Null or empty = no allow-list (all compatible plugins allowed). */
    public Set<String> getPluginsEffective() {
        if (plugins == null || plugins.isEmpty()) {
            return null;
        }
        return Set.copyOf(plugins);
    }
}

