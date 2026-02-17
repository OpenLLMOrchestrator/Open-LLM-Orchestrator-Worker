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
package com.openllmorchestrator.worker.contract;

/**
 * Well-known plugin type strings. Use these in {@link PluginTypeDescriptor#getPluginType()}
 * and when filtering "available tools" (e.g. {@link PluginTypes#TOOL}) for the planner.
 * Values align with pipeline config {@code pluginType} and the worker's allowed types.
 */
public final class PluginTypes {

    public static final String ACCESS_CONTROL = "AccessControlPlugin";
    public static final String TENANT_POLICY = "TenantPolicyPlugin";
    public static final String RATE_LIMIT = "RateLimitPlugin";
    public static final String MEMORY = "MemoryPlugin";
    public static final String VECTOR_STORE = "VectorStorePlugin";
    public static final String MODEL = "ModelPlugin";
    public static final String MCP = "MCPPlugin";
    /** Use this type for plugins that should be exposed as "available tools" to the planner. */
    public static final String TOOL = "ToolPlugin";
    public static final String FILTER = "FilterPlugin";
    public static final String GUARDRAIL = "GuardrailPlugin";
    public static final String REFINEMENT = "RefinementPlugin";
    public static final String PROMPT_BUILDER = "PromptBuilderPlugin";
    public static final String OBSERVABILITY = "ObservabilityPlugin";
    public static final String TRACING = "TracingPlugin";
    public static final String BILLING = "BillingPlugin";
    public static final String FEATURE_FLAG = "FeatureFlagPlugin";
    public static final String AUDIT = "AuditPlugin";
    public static final String SECURITY_SCANNER = "SecurityScannerPlugin";
    public static final String CACHING = "CachingPlugin";
    public static final String SEARCH = "SearchPlugin";
    public static final String LANG_CHAIN_ADAPTER = "LangChainAdapterPlugin";
    public static final String AGENT_ORCHESTRATOR = "AgentOrchestratorPlugin";
    public static final String WORKFLOW_EXTENSION = "WorkflowExtensionPlugin";
    public static final String CUSTOM_STAGE = "CustomStagePlugin";
    /** Evaluate model output (scores, metrics). Future-ready for quality gates and learning triggers. */
    public static final String EVALUATION = "EvaluationPlugin";
    /** Collect user feedback (ratings, corrections) for learning. Future-ready for incremental learning. */
    public static final String FEEDBACK = "FeedbackPlugin";
    /** Incremental learning: fine-tune, update embeddings, train on new data. Future-ready for model learning. */
    public static final String LEARNING = "LearningPlugin";
    /** Build/curate dataset from feedback and evaluations for training. */
    public static final String DATASET_BUILD = "DatasetBuildPlugin";
    /** Trigger training job when conditions are met (e.g. fine-tune, LoRA). */
    public static final String TRAIN_TRIGGER = "TrainTriggerPlugin";
    /** Register or promote a trained model for serving. */
    public static final String MODEL_REGISTRY = "ModelRegistryPlugin";
    /** Condition plugin for group-level if/elseif/else: reads context, returns output key {@code branch} (0=then, 1=elseif, ..., n-1=else). */
    public static final String CONDITION = "ConditionPlugin";

    private PluginTypes() {}
}
