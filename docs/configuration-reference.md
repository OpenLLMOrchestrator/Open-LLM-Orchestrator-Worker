# Engine Configuration Reference

This document describes **every field** in the engine configuration file. The config file is chosen by **`CONFIG_KEY`**: when `CONFIG_FILE_PATH` is unset, the path is **`config/<CONFIG_KEY>.json`** (e.g. `config/default.json`). Config is loaded in order Redis → DB → file. So that:

- Config files can be written or generated correctly.
- A **drag-and-drop UI** can create pipelines and enable feature flags without guessing.
- Validation rules and allowed values are explicit.

The config is **JSON**. All keys are case-sensitive. Unknown keys are ignored (`JsonIgnoreProperties`).

**For Configuration UI and Stage Debugging UI:** See [**ui-reference.md**](ui-reference.md) for a single source of truth: predefined stages table, plugin types table, config schema at a glance, pipeline structure, activity naming, and context keys for debugging.

---

## 1. Top-level structure

| Key | Type | Required | Description |
|-----|------|----------|-------------|
| `configVersion` | string | No | Schema version (e.g. `"1.0"`). Informational. |
| `enabledFeatures` | array of string | No | **Feature flags** to enable. See §2. Empty or omitted = no optional features. |
| `worker` | object | Yes* | Worker and task queue. See §3. |
| `temporal` | object | No | Temporal server connection. See §4. |
| `activity` | object | No | Activity defaults (timeouts, retry, payload limits). See §5. |
| `stageOrder` | array of string | No | **Execution order** of stage names. See §6. Used when using `root`/`rootByStage`. |
| `stagePlugins` | object | No | Stage name → plugin/activity id. See §7. |
| `mergePolicies` | object | No | Merge policy name → implementation. See §8. |
| `pipelines` | object | **Yes** | Named pipelines. At least one required. See §9. |
| `plugins` | array of string | No | Allowed plugin names for static pipelines and dynamic use (PLANNER, PLAN_EXECUTOR). See §10. |
| `dynamicPlugins` | object | No | Plugin name → JAR path (one handler per JAR). See §11. |
| `dynamicPluginJars` | array of string | No | JAR paths; each JAR is loaded for **all** StageHandler implementations and each is registered by its `name()`. See §11. |
| `queueTopology` | object | No | Queue topology for concurrency isolation. See §12. |

\* Worker may be merged from environment at runtime.

**Redis and database** are not in the config file. They are taken from **environment variables** (Docker/production) with **development defaults** when unset: `REDIS_HOST` (default `localhost`), `REDIS_PORT` (`6379`), `REDIS_PASSWORD` (empty), `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`. Config **file path** is **`config/<CONFIG_KEY>.json`** when `CONFIG_FILE_PATH` is unset; **`CONFIG_KEY`** (default `default`) also selects the Redis key `olo:engine:config:<CONFIG_KEY>:<version>`. See [config-reference.md](config-reference.md) for the full env table.

---

## 2. Feature flags (`enabledFeatures`)

**Type:** Array of strings.  
**Default:** Empty = no optional features run (only core pipeline execution).

Each string must be one of the following. **Only listed features execute**; others run no code (performance).

| Flag | Description |
|------|-------------|
| `HUMAN_SIGNAL` | Human-in-the-loop: suspend workflow, receive signal, resume with payload. |
| `STREAMING` | Streaming stage API: StreamObserver, token/intermediate updates. |
| `AGENT_CONTEXT` | Durable agent identity: AgentContext, memory store. |
| `DETERMINISM_POLICY` | Freeze model params, persist tool/retrieval outputs, randomness seed. |
| `CHECKPOINTABLE_STAGE` | Checkpointable stages: resumeFrom, branchFrom. |
| `OUTPUT_CONTRACT` | Output schema validation, enforceStrict. |
| `EXECUTION_GRAPH` | DAG execution; topological order. When off, linear stageOrder only. |
| `STAGE_RESULT_ENVELOPE` | StageMetadata, DependencyRef, deterministic flag on StageResult. |
| `VERSIONED_STATE` | stepId, executionId, immutable state per step. |
| `INTERCEPTORS` | Interceptor layer: beforeStage, afterStage, onError. |
| `PLANNER_PLAN_EXECUTOR` | Dynamic plan in context; kernel runs sub-plan. |
| `EXECUTION_SNAPSHOT` | snapshot(), ContextSnapshot for observability/recovery. |
| `POLICY_ENGINE` | ExecutionPolicyResolver: model by tenant tier, tool whitelist, caps. |
| `BUDGET_GUARDRAIL` | Kernel-level cost/token/iteration caps; stop when exceeded. |
| `CONCURRENCY_ISOLATION` | Queue-per-stage / queue-per-tenant topology. |
| `SECURITY_HARDENING` | Prompt injection defense, tool allowlist, output scanning, context poisoning. |
| `PLAN_SAFETY_VALIDATION` | Validate dynamic plan: allowed stages, max depth, no cycles. |
| `EXECUTION_GRAPH_EXPORT` | Export graph to DOT, Mermaid, JSON. |

**UI hint:** Provide a multi-select or checklist of the 18 flags; store as string array.

**Example:**
```json
"enabledFeatures": [
  "VERSIONED_STATE",
  "STAGE_RESULT_ENVELOPE",
  "EXECUTION_GRAPH",
  "PLANNER_PLAN_EXECUTOR",
  "INTERCEPTORS",
  "EXECUTION_SNAPSHOT"
]
```

---

## 3. Worker (`worker`)

| Key | Type | Required | Description |
|-----|------|----------|-------------|
| `queueName` | string | Yes | Temporal task queue name (e.g. `"core-task-queue"`). |
| `strictBoot` | boolean | No | If true, boot fails on config/plugin errors. Default: false. |

**Example:**
```json
"worker": {
  "queueName": "core-task-queue",
  "strictBoot": false
}
```

---

## 4. Temporal (`temporal`)

| Key | Type | Required | Description |
|-----|------|----------|-------------|
| `target` | string | No | Server address (e.g. `"localhost:7233"`). Default: `"localhost:7233"`. |
| `namespace` | string | No | Temporal namespace. Default: `"default"`. |

---

## 5. Activity (`activity`)

| Key | Type | Required | Description |
|-----|------|----------|-------------|
| `payload` | object | No | Limits for minimal Temporal history. See §5.1. |
| `defaultTimeouts` | object | No | Default timeouts in seconds. See §5.2. |
| `retryPolicy` | object | No | Default retry. See §5.3. |

### 5.1 Activity payload (`activity.payload`)

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `maxAccumulatedOutputKeys` | number | 0 | Max keys in input/accumulatedOutput to activities. 0 = no limit. |
| `maxResultOutputKeys` | number | 0 | Max keys in stage result output. 0 = no limit. |

**UI hint:** Optional; 0 means “no limit”. Use for “minimal history” tuning.

### 5.2 Default timeouts (`activity.defaultTimeouts`)

All values in **seconds**.

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `scheduleToStartSeconds` | number | — | Max wait for activity to start. |
| `startToCloseSeconds` | number | 30 | Max duration of activity execution. |
| `scheduleToCloseSeconds` | number | — | Max total time from schedule to close. |

### 5.3 Retry policy (`activity.retryPolicy`)

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `maximumAttempts` | number | 3 | Max retry attempts. |
| `initialIntervalSeconds` | number | 1 | First retry delay. |
| `backoffCoefficient` | number | 2.0 | Multiplier for next interval. |
| `maximumIntervalSeconds` | number | 60 | Cap on retry interval. |
| `nonRetryableErrors` | array of string | [] | Exception class names that must not be retried. |

---

## 6. Stage order (`stageOrder`)

**Type:** Array of strings.  
**Default:** If null/empty, predefined order in code is used.

**Allowed values (predefined stages):**  
`ACCESS`, `PRE_CONTEXT_SETUP`, `PLANNER`, `PLAN_EXECUTOR`, `EXECUTION_CONTROLLER`, `ITERATIVE_BLOCK`, `MODEL`, `RETRIEVAL`, `RETRIEVE`, `TOOL`, `MCP`, `MEMORY`, `REFLECTION`, `SUB_OBSERVABILITY`, `SUB_CUSTOM`, `ITERATIVE_BLOCK_END`, `FILTER`, `POST_PROCESS`, `EVALUATION`, `EVALUATE`, `FEEDBACK`, `FEEDBACK_CAPTURE`, `LEARNING`, `DATASET_BUILD`, `TRAIN_TRIGGER`, `MODEL_REGISTRY`, `OBSERVABILITY`, `CUSTOM`.  
Full table with descriptions and order: [ui-reference.md §1](ui-reference.md#1-predefined-stages-for-config-and-debugging).

**Semantics:** When a pipeline uses `root` (as stage-name → GROUP map, i.e. rootByStage), only stages **present in this list** and **present in the pipeline root** are included, in this order. Stages not in the list or not in the root are skipped.

**UI hint:** Ordered list; drag to reorder. Options = predefined stage names + custom (if supported).

---

## 7. Stage plugins (`stagePlugins`)

**Type:** Object. Keys = stage name, value = plugin/activity id (string).

**Example:** `"ACCESS": "default"`, `"MODEL": "com.example.MyModelPlugin"`.

Pipeline-level `stagePlugins` override this when set on the pipeline.

---

## 8. Merge policies (`mergePolicies`)

**Type:** Object. Keys = policy name, value = implementation (built-in name or FQCN).

**Built-in names:** `LAST_WINS`, `FIRST_WINS`, `PREFIX_BY_ACTIVITY`, `ALL_MODELS_RESPONSE_FORMAT`.

**Example:**
```json
"mergePolicies": {
  "DEFAULT_ASYNC": "LAST_WINS",
  "PREFIX_KEYS": "PREFIX_BY_ACTIVITY"
}
```

Referenced in pipeline/group as `asyncOutputMergePolicy` (string) or via `mergePolicy` hook.

---

## 9. Pipelines (`pipelines`)

**Type:** Object. Keys = pipeline id (e.g. `"default"`, `"rag-mistral"`), value = **pipeline section** object.

**At least one pipeline is required.**

### 9.1 Pipeline section (one pipeline)

Each pipeline can be defined in three ways (mutually exclusive in practice):

| Key | Type | Description |
|-----|------|-------------|
| `defaultTimeoutSeconds` | number | Default activity timeout for this pipeline. |
| `defaultAsyncCompletionPolicy` | string | Default for ASYNC groups. See §9.2. |
| `defaultMaxGroupDepth` | number | Max nesting depth for GROUP (default 5). |
| `mergePolicy` | object | Default merge policy hook. See §9.3. |
| `stagePlugins` | object | Stage name → plugin id (overrides engine-level). |
| **`root`** | object | **Either** a single GROUP tree **or** a map of stage name → GROUP. See §9.4. |
| **`stages`** | array | Alternative: list of stage blocks. See §9.5. |

**Validation:** Pipeline must have **root** (as tree or rootByStage) **or** **stages**; otherwise invalid.

### 9.2 Async completion policy

**Allowed values:** `ALL` | `FIRST_SUCCESS` | `FIRST_FAILURE` | `ALL_SETTLED`.

- `ALL` – Wait for all activities in the group.
- `FIRST_SUCCESS` – Proceed on first success.
- `FIRST_FAILURE` – Proceed on first failure.
- `ALL_SETTLED` – Wait until all complete or fail.

### 9.3 Merge policy hook (`mergePolicy`)

| Key | Type | Description |
|-----|------|-------------|
| `type` | string | Should be `"MERGE_POLICY"`. |
| `pluginType` | string | e.g. `"MergePolicy"`. |
| `name` | string | Activity/plugin name or FQCN (e.g. `"LAST_WINS"`). |

### 9.4 Root (two shapes)

**Shape A – Single root tree (legacy):**  
`root` is an object **with** a `"type"` field (e.g. `"GROUP"`). It is one node (GROUP or STAGE) with nested `children`. Used for a single tree.

**Shape B – Root by stage (recommended for drag-and-drop):**  
`root` is an object **without** a `"type"` field. Each key is a **stage name** (e.g. `FILTER`, `MODEL`), each value is a **GROUP** node (must have `"type": "GROUP"`). Execution order follows `stageOrder`; only stages present as keys are included.

**Example (root by stage):**
```json
"root": {
  "RETRIEVAL": {
    "type": "GROUP",
    "executionMode": "SYNC",
    "children": [
      {
        "type": "STAGE",
        "pluginType": "VectorStorePlugin",
        "name": "com.openllmorchestrator.worker.plugin.vectordb.VectorStoreRetrievalPlugin"
      }
    ]
  },
  "MODEL": {
    "type": "GROUP",
    "executionMode": "SYNC",
    "children": [
      {
        "type": "STAGE",
        "pluginType": "ModelPlugin",
        "name": "com.openllmorchestrator.worker.plugin.llm.Llama32ModelPlugin",
        "timeoutSeconds": 120
      }
    ]
  }
}
```

**UI hint:** For “root by stage”, one card per stage; each card is a GROUP with one or more STAGE children. Order of stages = `stageOrder` (or subset).

### 9.5 Node: GROUP and STAGE

Used inside `root` (tree or rootByStage) and inside `stages` → `groups` → children.

**Common fields:**

| Key | Type | Applies to | Description |
|-----|------|------------|-------------|
| `type` | string | **Required** | `"GROUP"` or `"STAGE"`. |
| `executionMode` | string | GROUP | `"SYNC"` or `"ASYNC"`. |
| `timeoutSeconds` | number | GROUP/STAGE | Override timeout (seconds). |
| `maxDepth` | number | GROUP | Max nested group depth. |
| `children` | array | GROUP | List of STAGE nodes or nested GROUP (as object). |

**GROUP-only:**

| Key | Type | Description |
|-----|------|-------------|
| `asyncCompletionPolicy` | string | `ALL` \| `FIRST_SUCCESS` \| `FIRST_FAILURE` \| `ALL_SETTLED`. |
| `asyncOutputMergePolicy` | string | Name from merge policy registry (e.g. `LAST_WINS`). |
| `mergePolicy` | object | Merge policy hook (type, pluginType, name). |
| `condition` | string | **If/elseif/else:** Plugin name (activity id) that runs first and must write output key `branch` (Integer: 0=then, 1=first elseif, …, n-1=else). When set, use `thenGroup`/`thenChildren`, `elseifBranches`, `elseGroup`/`elseChildren`. Prefer `thenGroup` and `elseGroup` (one GROUP each); condition has group as children. |
| `thenChildren` | array | When `condition` is set: GROUP/STAGE nodes for the “then” branch. If omitted, `children` is used as then. |
| `elseifBranches` | array | When `condition` is set: list of `{ "condition": "<plugin>", "then": [ GROUP/STAGE nodes ] }`. Evaluated in order; first branch whose condition plugin returns that index runs. |
| `elseChildren` | array | When `condition` is set: GROUP/STAGE nodes for the “else” branch. |

**STAGE-only:**

| Key | Type | Description |
|-----|------|-------------|
| `name` | string | **Required.** FQCN of plugin class or activity id. |
| `pluginType` | string | **Required.** One of allowed plugin types. See §9.6. |
| `scheduleToStartSeconds` | number | Activity timeout override. |
| `scheduleToCloseSeconds` | number | Activity timeout override. |
| `retryPolicy` | object | Same shape as `activity.retryPolicy`. |

**Allowed plugin types (`pluginType`):**  
`AccessControlPlugin`, `TenantPolicyPlugin`, `RateLimitPlugin`, `MemoryPlugin`, `VectorStorePlugin`, `ModelPlugin`, `MCPPlugin`, `ToolPlugin`, `FilterPlugin`, `GuardrailPlugin`, `RefinementPlugin`, `EvaluationPlugin`, `FeedbackPlugin`, `LearningPlugin`, `DatasetBuildPlugin`, `TrainTriggerPlugin`, `ModelRegistryPlugin`, `PromptBuilderPlugin`, `ObservabilityPlugin`, `TracingPlugin`, `BillingPlugin`, `FeatureFlagPlugin`, `AuditPlugin`, `SecurityScannerPlugin`, `CachingPlugin`, `SearchPlugin`, `LangChainAdapterPlugin`, `AgentOrchestratorPlugin`, `WorkflowExtensionPlugin`, `CustomStagePlugin`, `ConditionPlugin`.  
Full table with typical stages: [ui-reference.md §2](ui-reference.md#2-plugin-types-for-stage-node-plugintype).  
**ConditionPlugin:** Used in GROUP when `condition` is set. Must return output key `branch` (Integer): 0 = then, 1 = first elseif, …, n−1 = else.

### 9.6 Pipeline `stages` (alternative to root)

**Type:** Array of **stage block** objects.

| Key | Type | Description |
|-----|------|-------------|
| `stage` | string | Stage name (e.g. ACCESS, MODEL). |
| `groups` | array | List of **group config** objects. See §9.7. |

**Group config (object in `groups`):**

| Key | Type | Description |
|-----|------|-------------|
| `executionMode` | string | `SYNC` or `ASYNC`. |
| `asyncCompletionPolicy` | string | For ASYNC: ALL, FIRST_SUCCESS, FIRST_FAILURE, ALL_SETTLED. |
| `asyncOutputMergePolicy` | string | Merge policy name. |
| `mergePolicy` | object | Merge policy hook. |
| `maxDepth` | number | Max nested depth. |
| `timeoutSeconds` | number | Override. |
| `children` | array | Each element: **string** (activity/plugin name) or **object** (nested group with same shape). When `condition` is set, this is the “then” branch if `thenChildren` is omitted. |
| `condition` | string | **If/elseif/else:** Condition plugin name. Plugin must write output key `branch` (0=then, 1=first elseif, …, n-1=else). |
| `thenChildren` | array | “Then” branch: strings (plugin names) or nested group objects. |
| `elseifBranches` | array | List of `{ "condition": "<plugin>", "then": [ strings or group objects ] }`. |
| `elseChildren` | array | “Else” branch. |

**UI hint:** For “stages” mode, list of stages; each stage has a list of groups; each group has a list of children (strings = plugin names, objects = nested groups). For conditional groups, prefer **group as children**: use `thenGroup`, `elseGroup`, and `elseifBranches[].thenGroup` (one GROUP per branch), or then/elseif/else branch editors with list of nodes.

---

## 10. Allowed plugins (`plugins`)

**Type:** Array of strings. Each string = plugin name (FQCN or activity id) that may be used **statically** in pipeline config and **dynamically** (e.g. in PLANNER / PLAN_EXECUTOR).

**Default:** Omitted or empty = no allow-list; all registered plugins that pass contract compatibility are allowed.

When set, at bootstrap the engine:

1. Resolves each listed name from the activity registry (built-in + `dynamicPlugins`).
2. Runs contract compatibility check (`ContractVersion.requireCompatible(handler)`).
3. Builds a **compatible-activity map**; only these plugins are used for resolution and for building static pipeline structure.

Static pipelines are built **after** this check: only stages whose plugin name is in the compatible map are included. Dynamic resolution (PLANNER, PLAN_EXECUTOR) uses the same map, so only compatible plugins can be invoked. If a name in `plugins` is not registered or fails compatibility, bootstrap fails.

**Example:**
```json
"plugins": [
  "com.openllmorchestrator.worker.plugin.vectordb.VectorStoreRetrievalPlugin",
  "com.openllmorchestrator.worker.plugin.llm.Llama32ModelPlugin",
  "com.openllmorchestrator.worker.plugin.output.AnswerFormatPlugin"
]
```

---

## 11. Dynamic plugins (`dynamicPlugins`, `dynamicPluginJars`)

**`dynamicPlugins`** — Object. Keys = plugin name (activity id), value = path to JAR (string). At bootstrap, each JAR is loaded and the **first** StageHandler is registered under that key. If the file is missing or load fails, a no-op wrapper is registered and a log is emitted.

**`dynamicPluginJars`** — Array of JAR paths. At bootstrap, each JAR is loaded and **all** StageHandler implementations (via ServiceLoader) are registered, each under its own `name()`. Use when one JAR provides multiple plugins (e.g. sample-plugins.jar). Plugins from these JARs should be listed in `plugins` if you use an allow-list.

---

## 12. Queue topology (`queueTopology`)

Used when feature flag `CONCURRENCY_ISOLATION` is enabled.

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `strategy` | string | `"SINGLE"` | `SINGLE` \| `QUEUE_PER_STAGE` \| `QUEUE_PER_TENANT`. |
| `stageToQueue` | object | {} | Stage name → task queue name. |
| `tenantToQueue` | object | {} | Tenant id → task queue name. |

**UI hint:** Optional section; strategy dropdown + two key-value editors for stageToQueue and tenantToQueue.

---

## 12. Validation summary (for UI)

- **Root:** At least one of `root` (tree or rootByStage) or `stages` per pipeline.
- **stageOrder:** If using root-by-stage, list must contain only valid stage names (predefined or custom).
- **rootByStage:** Every value must be a GROUP node (`"type": "GROUP"`).
- **STAGE nodes:** Must have `name` and `pluginType`; `pluginType` must be from allowed list.
- **enabledFeatures:** Each value must be a valid `FeatureFlag` name (see §2).
- **Merge policy names:** Must exist in `mergePolicies` or be a known built-in when referenced.

---

## 13. Minimal valid config (single pipeline)

```json
{
  "configVersion": "1.0",
  "worker": {
    "queueName": "core-task-queue",
    "strictBoot": false
  },
  "stageOrder": ["ACCESS", "MODEL", "POST_PROCESS"],
  "pipelines": {
    "default": {
      "defaultTimeoutSeconds": 90,
      "defaultAsyncCompletionPolicy": "ALL",
      "root": {
        "ACCESS": {
          "type": "GROUP",
          "executionMode": "SYNC",
          "children": [
            {
              "type": "STAGE",
              "pluginType": "AccessControlPlugin",
              "name": "com.example.AccessPlugin"
            }
          ]
        },
        "MODEL": {
          "type": "GROUP",
          "executionMode": "SYNC",
          "children": [
            {
              "type": "STAGE",
              "pluginType": "ModelPlugin",
              "name": "com.example.MyModelPlugin"
            }
          ]
        }
      }
    }
  }
}
```

---

## 14. Drag-and-drop UI mapping

| UI concept | Config path | Notes |
|------------|-------------|--------|
| **Feature flags panel** | `enabledFeatures` | Multi-select; values = §2 table. |
| **Pipeline list** | `pipelines` | Keys = pipeline ids. Add/remove pipelines. |
| **Pipeline canvas (by stage)** | `pipelines.<id>.root` (rootByStage shape) | One column/card per stage; order from `stageOrder`. |
| **Stage card** | One key in `root` | Key = stage name; value = GROUP with `children`. |
| **Add stage to pipeline** | Add key to `root` + add to `stageOrder` if missing. | |
| **Group mode (SYNC/ASYNC)** | `root.<stage>.executionMode` | Dropdown SYNC | ASYNC. |
| **Add plugin to group** | Append to `root.<stage>.children` | New STAGE object with `type`, `name`, `pluginType`. |
| **Plugin picker** | — | Options = registered plugins / allowed plugin types + name (FQCN or activity id). |
| **Async merge policy** | `root.<stage>.asyncOutputMergePolicy` or `mergePolicy` | Dropdown from `mergePolicies` keys + built-ins. |
| **Activity timeout** | `root.<stage>.children[i].timeoutSeconds` or defaultTimeouts | Number input (seconds). |
| **Worker queue** | `worker.queueName` | Text. |
| **Payload limits** | `activity.payload` | Optional; two numbers (0 = no limit). |
| **Queue topology** | `queueTopology` | Optional; strategy + two maps. |

This reference plus the validation rules above are enough to drive a drag-and-drop pipeline and feature-flag UI that produces valid engine config JSON (e.g. for `config/<CONFIG_KEY>.json`). For a consolidated reference (stages, plugin types, config schema, and **stage debugging** — activity names, context keys, execution flow), use [**ui-reference.md**](ui-reference.md).
