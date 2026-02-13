# Engine configuration reference

All engine behaviour is driven by configuration. **Nothing is hardcoded in the engine.**

---

## Community reference configs (3 versions)

Three example configuration files are provided for different use cases. Copy and adapt as needed.

| File | Use case |
|------|----------|
| [`docs/config-examples/engine-config-minimal.json`](config-examples/engine-config-minimal.json) | **Minimal** — Bare minimum: worker queue + pipeline with `root` (GROUP/STAGE tree). No temporal, redis, database, or activity overrides. Good for local quick start. |
| [`docs/config-examples/engine-config-stages.json`](config-examples/engine-config-stages.json) | **Stages-based** — Top-level flow using `pipeline.stages`. Each stage has groups (SYNC/ASYNC); group children are **activity names** (plugin ids). Shows ACCESS → MEMORY → RETRIEVAL → MODEL → OBSERVABILITY with real plugin names. |
| [`docs/config-examples/engine-config-full.json`](config-examples/engine-config-full.json) | **Full** — All sections populated: worker, temporal, activity (timeouts + retry), redis, database, pipeline with `root` and full `stagePlugins`. Use as a production-style reference; in production, queue/Redis/DB are overridden from environment. |

---

## Production: environment variables (container)

In production, **queue name, Redis, and DB connection** come **only from environment** (or system properties). They are never read from the config file or from Redis/DB config storage.

| Env var | Default | Description |
|---------|---------|-------------|
| `QUEUE_NAME` | `core-task-queue` | Temporal task queue name. |
| `REDIS_HOST` | `localhost` | Redis host for config storage and cache. |
| `REDIS_PORT` | `6379` | Redis port. |
| `REDIS_PASSWORD` | (empty) | Redis password. |
| `DB_URL` | `jdbc:postgresql://localhost:5432/olo_config` | JDBC URL for config storage. |
| `DB_USERNAME` | `postgres` | DB user. |
| `DB_PASSWORD` | `postgres` | DB password. |
| `CONFIG_RETRY_SLEEP_SECONDS` | `30` | Seconds to sleep before retrying when config is not found anywhere. |
| `CONFIG_FILE_PATH` | `config/engine-config.json` | Mounted config file path (fallback when Redis and DB have no config). |

**Server configuration** (temporal target, activity timeouts/retry, pipeline) is **not** from env; it is loaded from the hierarchy below.

---

## Config loading order (bootstrap)

Config is **retrieved once** at bootstrap, in this order:

1. **Redis** – key `olo:engine:config` (JSON blob).
2. **DB** – table `olo_config` (created if missing): `config_key VARCHAR(255) PRIMARY KEY`, `config_value TEXT`. Engine config is stored with `config_key = 'engine_config'`.
3. **Mounted file** – path from `CONFIG_FILE_PATH` (or default).

If **not found** in any of the three, the process **sleeps** for `CONFIG_RETRY_SLEEP_SECONDS`, then **retries** (Redis → DB → file) until config is found.

When config is **found in the mounted file**, it is **written to Redis and DB** so the next boot (or other instances) can read it from Redis or DB.

After config is loaded, **connection values** (queue name, Redis, DB) are **overridden from env**. Only then does the worker **connect to Temporal** and register. So Temporal registration happens **only after** a valid config is available.

### Execution hierarchy: build once, reuse for container lifecycle

The **execution hierarchy** (stage plan, resolver, and plugin/activity registries) is **built once** at bootstrap from config and **reused for the entire container lifecycle**. It is **immutable** and holds **no transactional or request-scoped data** (no workflow ID, request context, or per-execution state). This avoids memory leaks: per-run state is passed as execution context at run time and is never retained by the runtime or by stage handlers.

---

## Root sections (in stored / file config)

| Section      | Purpose |
|-------------|---------|
| `configVersion` | Schema version (e.g. `"1.0"`). |
| `worker`    | Task queue and worker behaviour. |
| `temporal`  | Temporal server connection. |
| `activity`  | Default activity timeouts and retry policy. |
| `redis`     | Redis connection (if used). |
| `database`  | DB connection (if used). |
| `pipeline`  | Pipeline definition and stage plugins. |

---

## `worker` (overridden by env in production)

| Key           | Type    | Description |
|---------------|---------|-------------|
| `queueName`   | string  | Temporal task queue name. In production, use env `QUEUE_NAME`. |
| `strictBoot`  | boolean | If true, fail startup on validation warnings. |

---

## `temporal`

| Key         | Type   | Default           | Description |
|-------------|--------|-------------------|-------------|
| `target`    | string | `localhost:7233`  | Temporal server address. |
| `namespace` | string | `default`         | Temporal namespace. |

---

## `activity`

Default timeouts and retry policy for all activities (overridable per stage in pipeline nodes).

### `activity.defaultTimeouts`

| Key                    | Type    | Default | Description |
|------------------------|--------|--------|-------------|
| `scheduleToStartSeconds` | int? | (none) | Max time from schedule to worker pickup. |
| `startToCloseSeconds`   | int  | 30     | Max time for activity execution. |
| `scheduleToCloseSeconds`| int? | (none) | Max time from schedule to completion. |

### `activity.retryPolicy`

| Key                     | Type     | Default | Description |
|-------------------------|----------|--------|-------------|
| `maximumAttempts`       | int      | 3      | Max retry attempts. |
| `initialIntervalSeconds`| int      | 1      | First retry delay. |
| `backoffCoefficient`    | double   | 2.0    | Exponential backoff factor. |
| `maximumIntervalSeconds`| int      | 60     | Cap on retry interval. |
| `nonRetryableErrors`    | string[] | []     | Error type names that must not be retried. |

---

## `pipeline`

| Key                         | Type   | Description |
|-----------------------------|--------|-------------|
| `defaultTimeoutSeconds`     | int    | Default start-to-close for stages. **Required.** |
| `defaultAsyncCompletionPolicy` | string | Default for ASYNC groups: see below. |
| `stagePlugins`              | object | Predefined stage name → plugin id (used when `root` is used). See *Predefined stages and plugins* below. |
| `root`                      | object | **Legacy.** Root pipeline node (GROUP or STAGE tree). Required if `stages` is not set. |
| `stages`                    | array  | **Top-level flow.** Ordered list of stage blocks; each stage has groups whose children are activity (plugin) names. Required if `root` is not set. |

### Async completion policy

For ASYNC groups, how to complete:

| Value            | Behaviour |
|------------------|-----------|
| `ALL`            | Wait for all activities; fail if any fails. (Default.) |
| `FIRST_SUCCESS`  | Complete when the first activity succeeds. |
| `FIRST_FAILURE`  | Fail as soon as one activity fails. |
| `ALL_SETTLED`    | Wait for all to finish, then fail if any failed. |

Can be set at pipeline level (`defaultAsyncCompletionPolicy`) or per GROUP node (`asyncCompletionPolicy`).

### Pipeline data flow (original input, accumulated output, current plugin output)

On every workflow execution each plugin receives:

- **Original input** — read-only map from `ExecutionCommand.input` (same for all plugins in the run).
- **Accumulated output** — map of output from all **previous** stages (read-only in the activity).
- **Current plugin output** — map the plugin writes into via `context.putOutput(key, value)`; after the stage this is merged into accumulated output for the next stage.

After every **SYNC** plugin execution, its output map is merged into the accumulated output (putAll). For **ASYNC** groups, the merge is controlled by `asyncOutputMergePolicy` (see below).

### ASYNC output key overwrite policy

For ASYNC groups, set `asyncOutputMergePolicy` (or `asyncOutputMergePolicy` in group config) to control how multiple plugin outputs are merged and avoid data loss:

| Value | Behaviour |
|-------|-----------|
| `LAST_WINS` (default) | Last writer overwrites; merge in definition order (putAll each). |
| `FIRST_WINS` | First finished job's output written first; later outputs do not overwrite keys (putIfAbsent). |
| `PREFIX_BY_ACTIVITY` | Each plugin's keys are prefixed by activity name (e.g. `MemoryPlugin.result`) so no overwrite. |

**Merge policy classes** (extensible): `OutputMergePolicy` (interface), `PutAllMergePolicy` (SYNC default), `FirstWriterWinsMergePolicy`, `LastWriterWinsMergePolicy`, `PrefixByActivityMergePolicy`, and `AsyncOutputMergePolicy` (enum: FIRST_WINS, LAST_WINS, PREFIX_BY_ACTIVITY). Add custom implementations for new merge strategies.

### Pipeline mode: stages (top-level flow)

When `pipeline.stages` is set, the pipeline is defined as:

1. **Stages** = top-level flow (e.g. ACCESS → MEMORY → RETRIEVAL → MODEL).
2. **Within each stage**: one or more **groups**, with **SYNC** or **ASYNC** execution, **recursively** (groups can nest).
3. **Within each group**: **children** are **activity names** (plugin ids), e.g. `AccessControlPlugin`, `RateLimitPlugin`.
4. **Each child** is implemented by **one plugin** (a `StageHandler` registered in the activity registry).

Example shape:

```json
"stages": [
  { "stage": "ACCESS", "groups": [
    { "executionMode": "SYNC", "children": ["AccessControlPlugin", "TenantPolicyPlugin", "RateLimitPlugin"] }
  ]},
  { "stage": "MEMORY", "groups": [
    { "executionMode": "ASYNC", "asyncCompletionPolicy": "ALL", "children": ["MemoryPlugin", "VectorStorePlugin"] }
  ]},
  { "stage": "MODEL", "groups": [
    { "executionMode": "SYNC", "children": ["ModelPlugin"] }
  ]}
]
```

Group `children` entries can be **strings** (activity name) or **nested group objects** (same shape: `executionMode`, `children`, etc.). Every string must be a registered plugin/activity name (see *Predefined stages and plugins* and activity registry).

### Predefined stages and plugins

**Predefined stages** (must have an entry in `stagePlugins` when used in the pipeline):

| Stage           | Typical plugin(s) |
|-----------------|-------------------|
| `ACCESS`        | AccessControlPlugin, TenantPolicyPlugin, RateLimitPlugin |
| `MEMORY`        | MemoryPlugin, VectorStorePlugin |
| `RETRIEVAL`     | VectorStorePlugin, SearchPlugin |
| `MODEL`         | ModelPlugin |
| `MCP`           | MCPPlugin |
| `TOOL`          | ToolPlugin |
| `FILTER`        | FilterPlugin, GuardrailPlugin |
| `POST_PROCESS`  | RefinementPlugin, PromptBuilderPlugin |
| `OBSERVABILITY` | ObservabilityPlugin, TracingPlugin, BillingPlugin, FeatureFlagPlugin, AuditPlugin |
| `CUSTOM`        | CustomStagePlugin (or custom bucket handlers) |

**Available plugin identifiers** (config maps each stage to one plugin id; default implementation uses `"default"`): AccessControlPlugin, TenantPolicyPlugin, RateLimitPlugin, MemoryPlugin, VectorStorePlugin, ModelPlugin, MCPPlugin, ToolPlugin, FilterPlugin, GuardrailPlugin, RefinementPlugin, PromptBuilderPlugin, ObservabilityPlugin, TracingPlugin, BillingPlugin, FeatureFlagPlugin, AuditPlugin, SecurityScannerPlugin, CachingPlugin, SearchPlugin, LangChainAdapterPlugin, AgentOrchestratorPlugin, WorkflowExtensionPlugin, CustomStagePlugin.

---

## Pipeline nodes (when using `root`)

When using `pipeline.root`, the tree is made of GROUP and STAGE nodes:

### STAGE node

| Key                     | Type    | Description |
|-------------------------|---------|-------------|
| `type`                  | string  | `"STAGE"`. **Required.** |
| `name`                  | string  | Stage name (predefined or custom). **Required.** |
| `timeoutSeconds`        | int?    | Override start-to-close for this stage. |
| `scheduleToStartSeconds`| int?    | Override schedule-to-start. |
| `scheduleToCloseSeconds`| int?    | Override schedule-to-close. |
| `retryPolicy`           | object? | Override retry policy (same shape as `activity.retryPolicy`). |

### GROUP node

| Key                    | Type   | Description |
|------------------------|--------|-------------|
| `type`                 | string | `"GROUP"`. **Required.** |
| `executionMode`        | string | `"SYNC"` or `"ASYNC"`. **Required.** |
| `asyncCompletionPolicy`| string?| For ASYNC: `ALL`, `FIRST_SUCCESS`, `FIRST_FAILURE`, `ALL_SETTLED`. |
| `asyncOutputMergePolicy` | string?| For ASYNC: `FIRST_WINS`, `LAST_WINS`, `PREFIX_BY_ACTIVITY` (output key overwrite). |
| `timeoutSeconds`       | int?   | Default timeout for children (ASYNC group). |
| `children`             | array  | Child nodes. **Required.** |

---

## Example (minimal)

```json
{
  "configVersion": "1.0",
  "worker": { "queueName": "core-task-queue" },
  "pipeline": {
    "defaultTimeoutSeconds": 30,
    "stagePlugins": { "ACCESS": "default", "MEMORY": "default", "RETRIEVAL": "default", "MODEL": "default", "MCP": "default", "TOOL": "default", "FILTER": "default", "POST_PROCESS": "default", "OBSERVABILITY": "default", "CUSTOM": "default" },
    "root": { "type": "GROUP", "executionMode": "SYNC", "children": [
      { "type": "STAGE", "name": "ACCESS", "timeoutSeconds": 10 },
      { "type": "STAGE", "name": "MODEL", "timeoutSeconds": 120 }
    ]}
  }
}
```

`temporal` and `activity` are optional; defaults are used when omitted.

### Example using `stages` (top-level flow, activity names as children)

```json
{
  "configVersion": "1.0",
  "worker": { "queueName": "core-task-queue" },
  "pipeline": {
    "defaultTimeoutSeconds": 30,
    "defaultAsyncCompletionPolicy": "ALL",
    "stages": [
      { "stage": "ACCESS", "groups": [
        { "executionMode": "SYNC", "children": ["AccessControlPlugin", "TenantPolicyPlugin", "RateLimitPlugin"] }
      ]},
      { "stage": "MEMORY", "groups": [
        { "executionMode": "ASYNC", "asyncCompletionPolicy": "ALL", "children": ["MemoryPlugin", "VectorStorePlugin"] }
      ]},
      { "stage": "MODEL", "groups": [
        { "executionMode": "SYNC", "children": ["ModelPlugin"] }
      ]}
    ]
  }
}
```
