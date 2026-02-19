# Design: Execution Tree and Feature Handlers at Bootstrap

This document describes the design for creating the execution tree at bootstrap and attaching feature handlers so that at runtime each node traversal invokes only enabled features in a fixed, bootstrap-defined order. Disabled features have no handler (null); the core execution path does a simple null check or skips them by not including them in the list, keeping the hot path fast.

**Per-queue:** The execution tree is built **per task queue**. When two queues are running, each can use a different template (config, pipelines, feature handlers). Config is loaded per queue (Redis key, file path, or DB key include queue name); bootstrap runs once per queue and registers the tree under that queue name. At runtime the workflow uses its task queue name for all lookups (plans, chain, feature flags, resolver).

---

## 0. Queue-specific template configuration and config keys

Each **task queue** has its own **template**: one `EngineFileConfig` (pipelines, capabilityOrder, capabilities, enabledFeatures, featurePlugins, etc.). There is **no fallback** to a shared “default” key: config is always stored and loaded by **queue name** in Redis and DB so that queue A and queue B have independent configurations.

### 0.1 Same key for read and write

| Source | Key / path | Notes |
|--------|------------|--------|
| **Redis** | `olo:engine:config:<queueName>:<version>` | e.g. `olo:engine:config:chat:1.0`, `olo:engine:config:doc:1.0`. Version from config or env `CONFIG_VERSION` (default `1.0`). |
| **DB** | Table `olo_config`, column `config_key` = `engine_config:<queueName>` | e.g. `engine_config:chat`, `engine_config:doc`. |
| **File** | `<configBasePath>/<queueName>.json` | e.g. `config/chat.json`, `config/doc.json`. When `CONFIG_FILE_PATH` is a file (e.g. `config/default.json`), base path is its parent directory. |

- **Load (worker bootstrap):** Worker calls `WorkerBootstrap.initialize(queueName)`. Queue name comes from env `QUEUE_NAME` or caller. Repos are built with that `queueName` and load from the keys above.
- **Fallback to default:** If queue-specific config is not available (key or file missing), each source falls back to the **default** so the worker can start:
  - **Redis:** try `olo:engine:config:default:<version>` when queue key returns nothing.
  - **DB:** try `config_key = engine_config` when `engine_config:<queueName>` returns nothing.
  - **File:** try `config/default.json` (or the single-file path from env) when `<queueName>.json` is missing or unreadable.
- **Write (CLI / dashboard / config service):** When saving a template for a queue, use the **same** `queueName` as the key:
  - Redis: `EngineConfigWriter.writeToRedis(config, redis, queueName)` → key `olo:engine:config:<queueName>:<version>`.
  - DB: use `config_key = engine_config:<queueName>` when persisting.
  - File: write to `config/<queueName>.json`.

So **queue-specific template** and **queue-specific config in Redis and DB** are the same thing: one config document per queue, keyed by queue name. When a queue has no config yet, **default** is used so a single shared config can serve multiple queues until they get their own.

### 0.2 One queue name everywhere

The **same** `queueName` is used for:

1. **Bootstrap load** — Redis/DB/file key (above).
2. **EngineRuntime** — execution tree is stored under `EngineRuntime.getQueueRuntime(queueName)`.
3. **Temporal worker registration** — `factory.newWorker(taskQueue, ...)` with `taskQueue = queueName`.
4. **Runtime** — workflow uses `Workflow.getInfo().getTaskQueue()` and passes it to `EngineRuntime.getConfig(queueName)`, `getCapabilityPlan(queueName, pipelineName)`, etc.

If no config is found for the queue and default is also missing, bootstrap retries until config appears. To give a queue its own template, create and store config under that queue’s key; otherwise it uses default until then.

---

## 1. Goals

- **Execution tree** is created once at **bootstrap** from config (pipelines → capability plans). No change here.
- **Feature handlers** are attached at bootstrap: for each **enabled** feature, a **feature handler object** is created and registered; for disabled features, **no handler** (null) so the core runtime does not pay any cost.
- **Map and list** are maintained at bootstrap:
  - **Map&lt;FeatureFlag, FeatureHandler&gt;** — lookup by feature (enabled → handler; disabled → absent or null).
  - **List&lt;FeatureHandler&gt;** — **ordered** list of the same handler instances so the **execution hierarchy** (sequence) is fixed at bootstrap and persists for the container lifecycle.
- **At runtime**, during **every node traversal** (each capability execution):
  - The kernel gives **every registered feature** (the ordered list) a chance to perform its task.
  - Handlers run in **bootstrap-defined list order** (before/after capability).
  - No per-node feature-flag checks: the list contains only enabled handlers; a null check per handler in the list is enough (or the list has no nulls).

---

## 2. Concepts

| Concept | Description |
|--------|-------------|
| **Execution tree** | The **CapabilityPlan** (ordered list of **CapabilityGroupSpec**). Built at bootstrap from config; **immutable** — never mutated at runtime. |
| **Feature handler** | Object that implements **FeatureHandler** (extends **ExecutionInterceptor**). Invoked before/after each capability and on error. One instance per enabled feature. |
| **FeatureHandlerRegistry** | Holds **Map&lt;FeatureFlag, FeatureHandler&gt;** and **List&lt;FeatureHandler&gt;** (ordered). Built once at bootstrap; immutable. Only enabled features appear; disabled features have no entry (no handler). |
| **Node traversal** | Each execution of one capability inside a group (SYNC: one after another; ASYNC: parallel). The “node” here is the capability invocation. |

---

## 3. Bootstrap Flow (per queue)

1. **Resolve queue name** → from `WorkerBootstrap.initialize(queueName)` or env `QUEUE_NAME` in `LoadConfigStep`.
2. **Load config for queue** → Redis key `olo:engine:config:<queueName>:<version>`, file `config/<queueName>.json`, or DB key `engine_config:<queueName>`. Merge with env (connection config).
3. **Build execution tree** → `CapabilityPlan` per pipeline for this queue (unchanged).
4. **Build feature handler registry** (for this queue):
   - For each **enabled** feature, obtain a **FeatureHandler** (from a **FeatureHandlerFactory** or a fixed mapping).
   - Put in **Map&lt;FeatureFlag, FeatureHandler&gt;** (feature → handler).
   - Append to **List&lt;FeatureHandler&gt;** in a **fixed order** (e.g. feature enum order or explicit list) so the execution hierarchy is deterministic.
   - Disabled features: **no** entry in map, **no** entry in list → no handler, no cost at runtime.
4. **Build execution interceptor chain** from **List&lt;FeatureHandler&gt;** (and any other interceptors). This chain is the “attachment” of feature handlers to the execution path: when the kernel runs, it uses this chain for before/after capability and onError.
5. **Set on EngineRuntime**:
   - `setFeatureHandlerRegistry(registry)` — map + ordered list.
   - `setExecutionInterceptorChain(chain)` — chain that includes the ordered feature handlers (and optionally other interceptors).

So the **execution tree** (plan) is separate; the **feature handler list** is part of the **interceptor chain** used whenever the plan is executed. The “attachment” is: the chain used by the kernel is built from the ordered list at bootstrap.

---

## 4. Immutable execution tree and execution-scoped copy

The **per-queue execution tree** (CapabilityPlan in EngineRuntime) is **immutable**. No code path mutates it.

- **Static flow:** The execution-scoped placeholder in context (`context.getExecutionPlan()`) stays **null**. The workflow uses the **immutable** global plan from `EngineRuntime.getCapabilityPlan(queueName, pipelineName)` for the whole run. No copy is created.

- **Planner or debug phase:** When a stage needs to modify the hierarchy (e.g. planner, debug session), the copy is created **only then**:
  1. The stage (e.g. planner) obtains the current plan: if `context.getExecutionPlan()` is already set, use it; otherwise use the global plan from `EngineRuntime.getCapabilityPlan(context.getQueueName(), context.getPipelineName())`.
  2. The stage creates a **copy** with `plan.copyForExecution()`, applies its changes to the copy, and sets it in context: `context.setExecutionPlan(copy)`. The copy is created only during this phase, not at workflow start.
  3. Subsequent kernel runs (e.g. on resume) use the plan from context when non-null: workflow passes `context.getExecutionPlan() != null ? context.getExecutionPlan() : globalPlan`.
  4. The global per-queue tree is never written to. Transactional/execution context is per execution, so the copy lives in that scope.

---

## 5. Runtime Flow

1. Workflow sets **queueName** on the command from `Workflow.getInfo().getTaskQueue()` (if not already set).
2. Workflow gets **CapabilityPlan** from `EngineRuntime.getCapabilityPlan(queueName, pipelineName)` (immutable global tree).
3. Workflow uses **effective plan**: `context.getExecutionPlan() != null ? context.getExecutionPlan() : globalPlan`. Static flow: placeholder is null, so immutable global plan is used. Planner/debug: a stage has set a copy in context, so that copy is used (e.g. on resume).
4. Workflow builds **KernelOrchestrator** with **CapabilityInvoker** and **ExecutionInterceptorChain** from `EngineRuntime.getExecutionInterceptorChain(queueName)` (chain = ordered feature handlers + any others for that queue).
5. Kernel runs the **effective plan**; for each group, the appropriate **GroupExecutor** (Sync/Async/etc.) runs.
6. For **each capability** inside the group:
   - **interceptorChain.beforeCapability(ctx)** → each registered feature handler runs in **list order** (no feature-flag check; list has only enabled handlers).
   - Invoke capability (invoker).
   - **interceptorChain.afterCapability(ctx, result)** → same list order.
   - On error: **interceptorChain.onError(ctx, e)**.

So **every node traversal** (each capability) gives **every registered feature** a chance to run in **bootstrap-defined sequence**. Disabled features are simply not in the list, so the core path stays fast (no per-node flag checks; optional single null check per handler if the list can have nulls, or strict no-null list).

---

## 6. API Summary

| Component | Responsibility |
|-----------|----------------|
| **FeatureHandler** | Extends **ExecutionInterceptor**. Adds **getFeature()** for identification. Implementations: before/after/onError. |
| **FeatureHandlerRegistry** | Immutable. **getHandler(FeatureFlag)** → handler or null. **getOrderedHandlers()** → **List&lt;FeatureHandler&gt;** (no nulls; order = bootstrap-defined). |
| **FeatureHandlerFactory** | **createHandler(FeatureFlag)** → FeatureHandler or null. Used at bootstrap to build the registry; returns null for “no handler” for that feature. |
| **EngineRuntime** | Per-queue: **getQueueRuntime(queueName)** → **QueueExecutionTree** (config, plans, resolver, featureFlags, featureHandlerRegistry, executionInterceptorChain). **getConfig(queueName)**, **getCapabilityPlan(queueName, pipelineName)**, **getExecutionInterceptorChain(queueName)**, **getFeatureFlags(queueName)**, **getCapabilityResolver(queueName)**. Null queueName → default queue (single registered or env QUEUE_NAME). |
| **Bootstrap step** | Build registry (from enabled features + factory); build chain from **registry.getOrderedHandlers()**; set registry and chain on EngineRuntime for **queueName**. |
| **CoreWorkflowImpl** | Set command queue from `Workflow.getInfo().getTaskQueue()`; pass **effective plan** to kernel: `context.getExecutionPlan() != null ? context.getExecutionPlan() : globalPlan` (static flow = global immutable; planner/debug = copy in context when set). Create kernel with **EngineRuntime.getExecutionInterceptorChain(queueName)** so the kernel uses the bootstrap-defined handler list for that queue. |
| **ExecutionContext** | **getExecutionPlan()** / **setExecutionPlan(CapabilityPlan)** — execution-scoped plan; **null** in static flow (use immutable global). Only set during planner or debug phase when a stage creates a copy and modifies it. |

---

## 7. Order of Feature Handlers

The **list** order defines the **execution hierarchy** of features (e.g. observability before security, or vice versa). Options:

- **Enum order** — use `FeatureFlag.values()` or a fixed array so order is stable.
- **Explicit order** — config or code defines an ordered list of feature names; registry builds the handler list in that order.

Recommendation: use an **explicit ordered list** in code (e.g. `FeatureHandlerOrder.ORDER`) so that adding a new feature does not change the order of existing ones and the hierarchy remains explicit.

---

## 8. Feature Execution Plugins (per-feature pre/post)

- **Plugin type**: Each feature can have zero or more **feature execution plugins** (contract: `FeatureExecutionPlugin` with `beforeFeature(ctx)` and `afterFeature(ctx)`).
- **Config**: Global section **featurePlugins** — map from feature name to list of plugin names (e.g. `STREAMING: ["auditPlugin", "tracerPlugin"]`). Plugin names match `FeatureExecutionPlugin.name()`.
- **Bootstrap**: Plugins are loaded via ServiceLoader; registry is built by name. For each enabled feature, config lists plugin names; resolved plugins are attached to that feature’s handler via **FeatureHandlerWithPlugins** (wrapper). Order of plugins is the list order in config.
- **Context**: All plugins receive **FeatureExecutionContext** (feature name, stepId, groupIndex, capability name, stateBefore, originalInput, accumulatedOutput, currentPluginOutput, putOutput). Built from CapabilityContext + ExecutionContext so plugins get full context data.
- **Runtime**: At each capability, for a feature with plugins: run all plugins’ **beforeFeature** (pre), then the feature handler, then all plugins’ **afterFeature** (post). Plugin failures are logged and do not block others.

---

## 9. Summary

- **Execution tree** = capability plan, built at bootstrap (unchanged).
- **Feature handlers** = one object per enabled feature, created at bootstrap; stored in **Map&lt;FeatureFlag, FeatureHandler&gt;** and **List&lt;FeatureHandler&gt;** (ordered).
- **Feature plugins** = optional per-feature list from config (global **featurePlugins**); attached at bootstrap; pre/post run with **FeatureExecutionContext** at every node.
- **Attachment** = the list is used to build the **ExecutionInterceptorChain** set on EngineRuntime; the kernel uses this chain at every capability execution.
- **Runtime** = every node (capability) traversal runs the ordered list of handlers (before/after/onError); each handler may wrap plugins that run pre/post with context data; disabled features are not in the list, so core execution stays fast.
