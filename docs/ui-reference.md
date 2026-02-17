# Configuration UI & Capability Debugging UI Reference

This document is a **structured reference** for building:

- **Configuration UI** — Forms, dropdowns, and pipeline editors that produce valid engine config (e.g. `config/<CONFIG_KEY>.json`).
- **Capability debugging UI** — Inspecting workflow runs: current capability, capability order, activity names, and context (input/output) at each step.

Use the tables and schemas below as the single source of truth for allowed values, field types, and execution semantics. Cross-references: [configuration-reference.md](configuration-reference.md) (full config schema), [config-reference.md](config-reference.md) (load order, env), [plugin-contract.md](plugin-contract.md) (plugin API).

**Capability flow:** Predefined capabilities (below) have a fixed order via **`capabilityOrder`** (or **`stageOrder`**). Users can define **custom capabilities** in config (**`capabilities`**: name → `{ "pluginType", "name" }`) and reference any capability—predefined or custom—anywhere in the flow.

---

## 1. Predefined capabilities (for config and debugging)

Use this table to:

- Populate **capabilityOrder** (or **stageOrder**) dropdowns and drag-and-drop capability lists.
- Show **capability labels and descriptions** in pipeline and debugging UIs.
- Resolve **aliases** (e.g. RETRIEVE → same semantics as RETRIEVAL).

| Capability ID | Name | Description | Alias of | Order | Suggested plugin types | Debugging hint |
|---------------|------|-------------|----------|-------|------------------------|----------------|
| ACCESS | ACCESS | Access control, auth, tenant check | — | 0 | AccessControlPlugin, TenantPolicyPlugin, RateLimitPlugin | First capability; check permissions before any work. |
| PRE_CONTEXT_SETUP | PRE_CONTEXT_SETUP | Set up context before planning/execution | — | 1 | (custom) | Context keys set here are visible to later capabilities. |
| PLANNER | PLANNER | LLM-driven plan (task decomposition) | — | 2 | (internal) | Plan is stored in context for PLAN_EXECUTOR. |
| PLAN_EXECUTOR | PLAN_EXECUTOR | Execute sub-plan from planner | — | 3 | (internal) | Runs capabilities dictated by dynamic plan. |
| EXECUTION_CONTROLLER | EXECUTION_CONTROLLER | Control execution flow | — | 4 | (custom) | Optional control point. |
| ITERATIVE_BLOCK | ITERATIVE_BLOCK | Start of iterative loop | — | 5 | (internal) | Loop boundary; children may run multiple times. |
| MODEL | MODEL | LLM invocation (chat/completion) | — | 6 | ModelPlugin | Output: response, result, or model-specific keys. |
| RETRIEVAL | RETRIEVAL | Retrieve from vector store / search | — | 7 | VectorStorePlugin | Output: retrievedChunks, embeddings, etc. |
| RETRIEVE | RETRIEVE | Same as RETRIEVAL | RETRIEVAL | 8 | VectorStorePlugin | Use for “retrieve” naming in learning pipelines. |
| TOOL | TOOL | Tool/function execution | — | 9 | ToolPlugin | Output: tool result per tool. |
| MCP | MCP | MCP tools/servers | — | 10 | MCPPlugin | Output: MCP-specific keys. |
| MEMORY | MEMORY | Read/write conversation or user state | — | 11 | MemoryPlugin | Input/output: memory keys; state across turns. |
| REFLECTION | REFLECTION | Reflect on output (e.g. self-critique) | — | 12 | (custom) | Often reads result, writes refined or meta output. |
| SUB_OBSERVABILITY | SUB_OBSERVABILITY | Per-capability observability | — | 13 | ObservabilityPlugin, TracingPlugin | Metrics/traces for this segment. |
| SUB_CUSTOM | SUB_CUSTOM | Custom sub-logic | — | 14 | CustomStagePlugin | Arbitrary logic. |
| ITERATIVE_BLOCK_END | ITERATIVE_BLOCK_END | End of iterative block | — | 15 | (internal) | Paired with ITERATIVE_BLOCK. |
| FILTER | FILTER | Filter, tokenize, chunk input | — | 16 | FilterPlugin, GuardrailPlugin | Output: tokenizedChunks, filtered input, etc. |
| POST_PROCESS | POST_PROCESS | Format, normalize, refine output | — | 17 | RefinementPlugin, AnswerFormatPlugin | Output: result, response, formatted text. |
| EVALUATION | EVALUATION | Score/measure model output | — | 18 | EvaluationPlugin | Output: score, metrics; quality gates. |
| EVALUATE | EVALUATE | Same as EVALUATION | EVALUATION | 19 | EvaluationPlugin | Use in learning pipelines. |
| FEEDBACK | FEEDBACK | Collect user feedback | — | 20 | FeedbackPlugin | Output: rating, correction, feedback payload. |
| FEEDBACK_CAPTURE | FEEDBACK_CAPTURE | Same as FEEDBACK | FEEDBACK | 21 | FeedbackPlugin | Use in learning pipelines. |
| LEARNING | LEARNING | Incremental learning step | — | 22 | LearningPlugin | Output: learningRun, model update metadata. |
| DATASET_BUILD | DATASET_BUILD | Build/curate dataset from feedback | — | 23 | DatasetBuildPlugin | Output: datasetId, sampleCount, etc. |
| TRAIN_TRIGGER | TRAIN_TRIGGER | Trigger training job | — | 24 | TrainTriggerPlugin | Output: jobId, status; triggers fine-tune/LoRA. |
| MODEL_REGISTRY | MODEL_REGISTRY | Register/promote trained model | — | 25 | ModelRegistryPlugin | Output: modelId, version; serving rollout. |
| OBSERVABILITY | OBSERVABILITY | Final observability | — | 26 | ObservabilityPlugin, TracingPlugin, AuditPlugin | Logging, metrics, audit. |
| CUSTOM | CUSTOM | Custom capability | — | 27 | CustomStagePlugin | Last predefined; custom logic. |

**Canonical capability order (array for default capabilityOrder or stageOrder):**  
`ACCESS`, `PRE_CONTEXT_SETUP`, `PLANNER`, `PLAN_EXECUTOR`, `EXECUTION_CONTROLLER`, `ITERATIVE_BLOCK`, `MODEL`, `RETRIEVAL`, `RETRIEVE`, `TOOL`, `MCP`, `MEMORY`, `REFLECTION`, `SUB_OBSERVABILITY`, `SUB_CUSTOM`, `ITERATIVE_BLOCK_END`, `FILTER`, `POST_PROCESS`, `EVALUATION`, `EVALUATE`, `FEEDBACK`, `FEEDBACK_CAPTURE`, `LEARNING`, `DATASET_BUILD`, `TRAIN_TRIGGER`, `MODEL_REGISTRY`, `OBSERVABILITY`, `CUSTOM`.

**Minimal learning flow (for “learning pipeline” preset):**  
`ACCESS`, `MEMORY`, `RETRIEVE`, `MODEL`, `EVALUATE`, `FEEDBACK_CAPTURE`, `DATASET_BUILD`, `TRAIN_TRIGGER`, `MODEL_REGISTRY`.

---

## 2. Plugin types (for PLUGIN node `pluginType`)

Use for **plugin type dropdown** in pipeline editor and for **filtering plugins by capability**. Each PLUGIN (leaf) node must have `pluginType` from this list and `name` = activity/plugin id (FQCN or short name). Legacy: `type` `"PLUGIN"` is accepted as alias for `"PLUGIN"`.

| Plugin type (value) | Description | Typical capabilities |
|---------------------|-------------|----------------|
| AccessControlPlugin | Access control, auth | ACCESS |
| TenantPolicyPlugin | Tenant policies, limits | ACCESS |
| RateLimitPlugin | Rate limiting | ACCESS |
| MemoryPlugin | Memory store | MEMORY |
| VectorStorePlugin | Vector DB store/retrieve | RETRIEVAL, RETRIEVE, FILTER |
| ModelPlugin | LLM invocation | MODEL |
| MCPPlugin | MCP tools | MCP |
| ToolPlugin | Tool execution | TOOL |
| FilterPlugin | Filtering, ingestion, tokenization | FILTER |
| GuardrailPlugin | Guardrails, safety | FILTER |
| RefinementPlugin | Output refinement | POST_PROCESS |
| EvaluationPlugin | Score/measure model output | EVALUATION, EVALUATE |
| FeedbackPlugin | Collect user feedback | FEEDBACK, FEEDBACK_CAPTURE |
| LearningPlugin | Incremental learning | LEARNING |
| DatasetBuildPlugin | Build dataset from feedback | DATASET_BUILD |
| TrainTriggerPlugin | Trigger training job | TRAIN_TRIGGER |
| ModelRegistryPlugin | Register/promote model | MODEL_REGISTRY |
| ConditionPlugin | Group if/elseif/else: return output key `branch` (0=then, 1=elseif, …, n-1=else) | (GROUP with `condition` set) |
| PromptBuilderPlugin | Prompt building | (often used inside MODEL) |
| ObservabilityPlugin | Observability | OBSERVABILITY, SUB_OBSERVABILITY |
| TracingPlugin | Tracing | OBSERVABILITY |
| BillingPlugin | Billing | OBSERVABILITY |
| FeatureFlagPlugin | Feature flags | (any) |
| AuditPlugin | Audit | OBSERVABILITY |
| SecurityScannerPlugin | Security scanning | FILTER, OBSERVABILITY |
| CachingPlugin | Caching | (any) |
| SearchPlugin | Search | RETRIEVAL, RETRIEVE |
| LangChainAdapterPlugin | LangChain adapter | (custom) |
| AgentOrchestratorPlugin | Agent orchestration | (custom) |
| WorkflowExtensionPlugin | Workflow extension | (custom) |
| CustomStagePlugin | Custom capability | CUSTOM, SUB_CUSTOM |

---

## 3. Config schema at a glance (for Configuration UI)

| Section | Type | Required | UI control |
|---------|------|----------|------------|
| configVersion | string | No | Text or readonly. |
| enabledFeatures | string[] | No | Multi-select; values = feature flags (§4). |
| worker | object | Yes | Form: queueName (text), strictBoot (checkbox). |
| temporal | object | No | Form: target, namespace. |
| activity | object | No | Nested: payload (numbers), defaultTimeouts (numbers), retryPolicy (numbers + string[]). |
| capabilityOrder / stageOrder | string[] | No | Ordered list; options = predefined capabilities (§1). |
| mergePolicies | object | No | Key-value; value = LAST_WINS \| FIRST_WINS \| PREFIX_BY_ACTIVITY \| FQCN. |
| pipelines | object | Yes | Pipeline list; each pipeline = form or canvas (§5). |
| plugins | string[] | No | Multi-select or list; values = allowed plugin names (FQCN/activity id). |
| dynamicPlugins | object | No | Key = plugin name, value = JAR path. |
| queueTopology | object | No | strategy dropdown + stageToQueue, tenantToQueue maps. |

---

## 4. Feature flags (for `enabledFeatures`)

| Flag | Description |
|------|-------------|
| HUMAN_SIGNAL | Human-in-the-loop: suspend, signal, resume. |
| STREAMING | Streaming stage API. |
| AGENT_CONTEXT | Durable agent identity. |
| DETERMINISM_POLICY | Reproducible runs. |
| CHECKPOINTABLE_STAGE | Checkpointable stages. |
| OUTPUT_CONTRACT | Output schema validation. |
| EXECUTION_GRAPH | DAG execution. |
| STAGE_RESULT_ENVELOPE | StageMetadata, DependencyRef. |
| VERSIONED_STATE | stepId, executionId. |
| INTERCEPTORS | beforeStage, afterStage, onError. |
| PLANNER_PLAN_EXECUTOR | Dynamic plan. |
| EXECUTION_SNAPSHOT | snapshot(), ContextSnapshot. |
| POLICY_ENGINE | ExecutionPolicyResolver. |
| BUDGET_GUARDRAIL | Cost/token/iteration caps. |
| CONCURRENCY_ISOLATION | Queue topology. |
| SECURITY_HARDENING | Prompt injection, allowlist. |
| PLAN_SAFETY_VALIDATION | Validate dynamic plan. |
| EXECUTION_GRAPH_EXPORT | Export graph (DOT, Mermaid, JSON). |

---

## 5. Pipeline structure (for pipeline editor)

- **Root by capability (recommended):** `pipelines.<id>.root` is an object whose **keys are capability names** (from §1) and values are **GROUP** nodes. Order of execution = order of capabilities in **capabilityOrder** (or **stageOrder**) (only capabilities present in root are run).
- **One capability = one card/column:** Each key in `root` is one capability; value must be `{ "type": "GROUP", "executionMode": "SYNC"|"ASYNC", "children": [ ... ] }`.
- **Children:** Array of **PLUGIN** nodes or nested **GROUP**. Each PLUGIN: `{ "PLUGIN", "name": "<activity id>", "pluginType": "<from §2>" }`. Optional: `timeoutSeconds`, `retryPolicy`, etc. Legacy: `"type": "PLUGIN"` accepted.
- **Async group options:** `asyncCompletionPolicy`: ALL | FIRST_SUCCESS | FIRST_FAILURE | ALL_SETTLED. `asyncOutputMergePolicy`: name from mergePolicies or built-in (LAST_WINS, FIRST_WINS, PREFIX_BY_ACTIVITY).

Validation: Every PLUGIN node must have `name` and `pluginType`; `pluginType` must be from §2; capability names in `root` and `capabilityOrder`/`stageOrder` should be from §1 (or custom if supported).

**Conditional groups (if/elseif/else):** On a GROUP node set `condition` to a plugin name (activity id). That plugin runs first and must write output key **`branch`** (Integer): 0 = then, 1 = first elseif, …, n−1 = else. **Within condition, use group as children:** prefer **`thenGroup`** (one GROUP), **`elseGroup`** (one GROUP), and **`elseifBranches[].thenGroup`** (one GROUP per branch). Alternatively use `thenChildren`, `elseChildren`, or `elseifBranches[].then` (lists). Only the selected branch runs. Use **ConditionPlugin** as `pluginType` for the condition plugin; stub: `StubConditionPlugin` (always returns branch 0).

---

## 6. Capability debugging UI reference

### 6.1 Activity name in Temporal and logs

- **Format:** `{stageBucketName}::{activityName}`.  
  Example: `MODEL::com.openllmorchestrator.worker.plugin.llm.Llama32ChatPlugin`.
- **stageBucketName** = capability name (e.g. MODEL, RETRIEVAL, EVALUATE). Use §1 to show a human-readable label.
- **activityName** = plugin `name()` (FQCN or short name). Use this to match config STAGE `name` and plugin registry.

**Debugging UI:** In Temporal activity list (or your run view), parse the activity type to show “Stage: MODEL”, “Plugin: Llama32ChatPlugin”. Link to pipeline config where this stage/plugin is defined.

### 6.2 Context keys (per execution)

- **originalInput** — Read-only. Keys from workflow input (e.g. `question`, `document`, `messages`, `pipelineName`). Same for all capabilities in the run.
- **accumulatedOutput** — Read-only at plugin entry. Merged output from all **previous** capabilities. Common keys (pipeline-dependent):
  - `tokenizedChunks`, `retrievedChunks` (after FILTER/RETRIEVAL)
  - `result`, `response` (after MODEL, POST_PROCESS)
  - `memory*`, plugin-specific keys
- **currentPluginOutput** — What this plugin writes via `context.putOutput(key, value)`. After the capability, this map is merged into accumulated output for the next capability.

**Debugging UI:** For “current stage” show: stage name (§1), activity name, and a snapshot of **originalInput** (collapsed), **accumulatedOutput** (expandable), and **currentPluginOutput** (or stage result output) after the stage completes.

### 6.3 Execution flow (for “stage timeline” or “run diagram”)

1. Order of capabilities = **capabilityOrder** (or **stageOrder**) (or pipeline root keys in that order). Only capabilities present in the pipeline root (and in capabilityOrder/stageOrder) run.
2. Within a capability: one or more **groups**; each group runs its **children** (SYNC = sequential, ASYNC = parallel). After ASYNC, merge policy runs.
3. Each child is one **activity** = one plugin. Activity type string = `stageBucketName::activityName` (see 6.1).

**Debugging UI:** Render a vertical or horizontal timeline of capabilities; for each capability show group(s) and activity names; for each activity show status (scheduled/running/completed/failed) and optional input/output summary (key names or truncated values).

### 6.4 Useful display fields per run

| Field | Source | Use in UI |
|-------|--------|-----------|
| Pipeline name | workflow input / ExecutionCommand | Title or breadcrumb. |
| Capability order | config capabilityOrder/stageOrder + pipeline root | List of “expected” stages. |
| Current / completed capability | Temporal activity history | Highlight current; checkmarks for completed. |
| Activity type | Activity type string | Parse to capability + plugin name. |
| Input (to activity) | originalInput + accumulatedOutput | Expandable “Input” panel. |
| Output (from activity) | Result / currentPluginOutput | “Output” or “Result” panel. |
| Error | Activity failure | Error message and stack. |

---

## 7. Merge policy built-ins (for dropdown)

| Value | Description |
|-------|-------------|
| LAST_WINS | Last writer overwrites (default). |
| FIRST_WINS | First finished; later outputs do not overwrite. |
| PREFIX_BY_ACTIVITY | Keys prefixed by activity name to avoid overwrite. |
| ALL_MODELS_RESPONSE_FORMAT | Special format for multi-model ASYNC. |

Custom: value in `mergePolicies` can be a FQCN; reference by the key in `asyncOutputMergePolicy`.

---

## 8. Async completion policy (for GROUP dropdown)

| Value | Description |
|-------|-------------|
| ALL | Wait for all activities; fail if any fails. |
| FIRST_SUCCESS | Complete when first activity succeeds. |
| FIRST_FAILURE | Fail as soon as one fails. |
| ALL_SETTLED | Wait for all; then fail if any failed. |

---

*This reference is intended to be stable so that Configuration UI and Capability Debugging UI can rely on it for validation, dropdowns, and display logic. When adding new predefined capabilities or plugin types, update this file and the code (PredefinedStages/PredefinedCapabilities, PluginTypes, configuration-reference.md) together.*
