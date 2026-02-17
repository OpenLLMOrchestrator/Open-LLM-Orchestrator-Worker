# Plugin Contract

This document is the **authoritative contract** for building plugins for the Open LLM Orchestrator Worker. Use it to implement, register, and scale plugins in a consistent way. The contract is **self-contained** so the community can build plugins without reading engine internals.

**Contract as a separate module:** The plugin API lives in the **`plugin-contract`** module (package `com.openllmorchestrator.worker.contract`). Both the **worker** (this repo) and **plugin implementations** (this repo’s **`plugins`** module or an external plugin repo) depend only on that contract. This keeps the worker and plugins decoupled and allows publishing the contract so external plugin repos can depend on it without depending on the full worker.

---

## 1. What is a plugin?

A **plugin** is a unit of work that runs at a specific **capability** in a pipeline. The engine executes capabilities in order; each capability can run one or more plugins (in SYNC or ASYNC groups). The engine resolves the plugin by **name** (the same name you use in pipeline config), invokes it with an **execution context**, and merges its **result** into the pipeline state.

- **Capability** = pipeline phase (e.g. `ACCESS`, `RETRIEVAL`, `MODEL`, `POST_PROCESS`). Order is defined by `capabilityOrder` (or legacy `stageOrder`) or pipeline structure.
- **Activity name** = the identifier used in config and registry. It is usually a **fully qualified class name (FQCN)** or a short name (e.g. `LAST_WINS` for a merge handler).
- **Plugin** = an implementation of `StageHandler` registered under that activity name. The engine looks up the handler by name and calls `execute(context)`.

No reflection is used at runtime: resolution is a **map lookup** by name. Plugins must be **registered at bootstrap** (built-in or from dynamic JARs).

---

## 2. Core contract: StageHandler (capability handler)

Every plugin must implement:

```text
package com.openllmorchestrator.worker.engine.stage;

public interface StageHandler {

    /** Unique name used in pipeline config and registry. Return a constant. */
    String name();

    /**
     * Execute the stage. Read from context; write output via context or return value.
     * Do not retain context after this method returns.
     */
    StageResult execute(ExecutionContext context);
}
```

### 2.1 `name()`

- Must return a **constant** that uniquely identifies this plugin.
- This is the **registration key** and the same value you put in pipeline config as the STAGE node’s `name` (e.g. FQCN or `"LAST_WINS"`).
- Used for logging, merge policies, and resolution.

### 2.2 `execute(ExecutionContext context)`

- **Input:** You receive a single `ExecutionContext` with:
  - **Read-only:** `getOriginalInput()` — initial pipeline input (same for all plugins).
  - **Read-only:** `getAccumulatedOutput()` — output from all previous stages (merged so far).
  - **Write:** `putOutput(key, value)` or `getCurrentPluginOutput().put(key, value)` — your stage’s output. The kernel merges this into accumulated state after your run.
- **Output:** Return a `StageResult` (see §4). The kernel also reads `context.getCurrentPluginOutput()` and merges it; if you return output in `StageResult`, it is merged as well. Prefer writing through `context.putOutput(...)` for consistency.
- **No retained state:** Do not hold a reference to `context` (or any mutable part of it) after `execute()` returns. The context is not valid after the call.

### 2.3 Threading and lifecycle

- The engine may call `execute` from a Temporal activity thread or workflow thread. Assume one call per execution; do not assume reuse of the same handler instance across runs unless you control that yourself.
- Handlers should be **stateless** per execution: use only context and local variables inside `execute()`.

---

## 3. ExecutionContext (input to the plugin)

The engine passes one `ExecutionContext` per invocation. You use it to read input and write output.

### 3.1 Methods you must use

| Method | Description |
|--------|-------------|
| `getOriginalInput()` | Read-only map: initial pipeline input (e.g. `question`, `document`, `messages`). Same for every plugin in the run. |
| `getAccumulatedOutput()` | Read-only map: outputs from all previous capabilities. Keys and conventions are pipeline-specific (e.g. `tokenizedChunks`, `retrievedChunks`, `result`, `response`). |
| `putOutput(key, value)` | Write your capability output. Same as `getCurrentPluginOutput().put(key, value)`. These entries are merged into accumulated state after your capability. |
| `getCurrentPluginOutput()` | Mutable map for this plugin’s output. Prefer `putOutput(key, value)` for clarity. |

### 3.2 Optional behavior (feature-flagged)

- **Human-in-the-loop:** `requestSuspendForSignal()` — request workflow to suspend until an external signal (e.g. human approval). Only effective when `HUMAN_SIGNAL` is enabled.
- **Pipeline break:** `requestPipelineBreak()` — request to stop the pipeline after this capability (no further capabilities run). For ASYNC groups, break happens only when all activities in the group request break (when so configured).
- **Agent context:** `getAgentContext()`, `setAgentContext(...)` — when `AGENT_CONTEXT` is enabled, for durable agent identity and memory.
- **Determinism:** `getDeterminismPolicy()`, `getRandomnessSeed()` — when `DETERMINISM_POLICY` is enabled, for reproducible runs.

You can also use `get(String key)` / `put(String key, Object value)` on the context’s generic state map for engine-internal data (e.g. `asyncStageResults` for merge handlers). Prefer documented keys.

### 3.3 What not to do

- Do not retain `context` (or its maps) after `execute()` returns.
- Do not mutate `getOriginalInput()` or `getAccumulatedOutput()`; they are intended read-only.

---

## 4. StageResult (output from the plugin)

Return a `StageResult` from `execute()`. The kernel uses both your returned result and `context.getCurrentPluginOutput()` to merge output.

### 4.1 Fields

| Field | Type | Description |
|-------|------|-------------|
| `stageName` | String | Usually same as `name()`. |
| `output` | Map<String, Object> | Canonical output (preferred). |
| `data` | Map<String, Object> | Deprecated; use `output`. Same as output for compatibility. |
| `metadata` | StageMetadata | Optional; for replay/audit (stepId, executionId, etc.). |
| `deterministic` | boolean | If true, result is replay-safe. |
| `dependencies` | List<DependencyRef> | Refs to prior steps/executions (for DAG/replay). |
| `requestPipelineBreak` | boolean | Request to stop the pipeline after this stage (see §3.2). |

### 4.2 Helpers

- `StageResult.withOutput(stageName, output)` — simple result with output map.
- `StageResult.deterministicEnvelope(stageName, output, metadata, dependencies)` — for deterministic envelope.
- `StageResult.builder().stageName(...).output(...).requestPipelineBreak(true).build()` — full control.

If you write output only via `context.putOutput(...)`, you can return:

```java
return StageResult.builder()
    .stageName(name())
    .output(new HashMap<>(context.getCurrentPluginOutput()))
    .requestPipelineBreak(context.isPipelineBreakRequested())
    .build();
```

---

## 5. Registration: how the engine finds your plugin

The engine resolves the handler by **activity name** (the value from pipeline config’s STAGE `name`). Resolution order:

1. **Predefined stage** — If the pipeline step is for a predefined stage (e.g. `ACCESS`, `MODEL`) and a plugin id is configured for that stage, the predefined bucket is used.
2. **Activity registry** — Map of activity name → `StageHandler`. This is where **your** plugin is registered (by FQCN or short name).
3. **Custom bucket** — For custom stage names.

So for pipeline config like:

```json
{
  "type": "STAGE",
  "pluginType": "VectorStorePlugin",
  "name": "com.openllmorchestrator.worker.plugin.vectordb.VectorStoreRetrievalPlugin"
}
```

the engine will call the activity with **stage name** = `"com.openllmorchestrator.worker.plugin.vectordb.VectorStoreRetrievalPlugin"`. The resolver looks up that string in the **activity registry**. Your plugin must be registered under exactly that name.

### 5.1 Built-in registration

At bootstrap, the worker builds an `ActivityRegistry` and registers known handlers by **name** (FQCN or short name). For example:

- `com.openllmorchestrator.worker.plugin.vectordb.VectorStoreRetrievalPlugin` → instance of `VectorStoreRetrievalPlugin`
- `com.openllmorchestrator.worker.plugin.llm.Llama32ModelPlugin` → instance of `Llama32ModelPlugin`
- `LAST_WINS` → instance of `LastWinsMergeHandler`

To add a new built-in plugin, register it in the same way in the bootstrap step that builds the activity registry (e.g. `BuildActivityRegistryStep`), using the **exact** string that pipeline config will use as `name`.

### 5.2 Contract module and dependency

- **Worker:** Depends on `project(':plugin-contract')` (or published `com.openllm:plugin-contract`).
- **Plugin repo / plugins module:** Depend only on **plugin-contract** (no worker dependency). Example (Gradle): `implementation project(':plugin-contract')` or `implementation 'com.openllm:plugin-contract:0.0.1'`.

Implement `com.openllmorchestrator.worker.contract.StageHandler` and receive `PluginContext` in `execute(PluginContext context)`.

### 5.3 Dynamic plugins (JAR)

You can ship a plugin in a **JAR** and register it by name and path in config:

```json
"dynamicPlugins": {
  "my.company.MyPlugin": "/path/to/my-plugin.jar"
}
```

- **Key** = plugin name (used as activity name in pipelines).
- **Value** = path to the JAR file.

The JAR must expose a **single** implementation of `StageHandler` via Java ServiceLoader:

- File: `META-INF/services/com.openllmorchestrator.worker.contract.StageHandler`
- Content: one line = fully qualified class name of your implementation.

If the file is missing or the JAR fails to load, the engine registers a **no-op wrapper** that logs and returns an empty result so the workflow can continue. Your plugin’s `name()` should return the same string as the key in `dynamicPlugins` so that config and registry stay in sync. The **plugins** module in this repo contains a sample plugin (`SampleEchoPlugin`) that depends only on **plugin-contract** and can be built as a JAR for the `plugins/` folder.

---

## 6. Pipeline config: how to reference your plugin

In the engine config (e.g. `config/default.json` or `config/<CONFIG_KEY>.json`), pipelines define stages and groups. Each STAGE node has:

- **`type`:** `"STAGE"`
- **`name`:** The activity name (FQCN or short name) that the registry uses. **Must match** the name you used when registering.
- **`pluginType`:** One of the allowed plugin type constants (see §7). Used for validation and UI; resolution is by `name`.

Example:

```json
{
  "type": "STAGE",
  "pluginType": "VectorStorePlugin",
  "name": "com.openllmorchestrator.worker.plugin.vectordb.VectorStoreRetrievalPlugin",
  "timeoutSeconds": 60
}
```

So: **name** = resolution key = your `StageHandler`’s registration name; **pluginType** = semantic type for the stage (must be from the allowed list).

---

## 7. Allowed plugin types

The config field `pluginType` must be one of the following (semantic only; resolution is by `name`):

| pluginType | Typical use |
|------------|-------------|
| AccessControlPlugin | Access control |
| TenantPolicyPlugin | Tenant policies |
| RateLimitPlugin | Rate limiting |
| MemoryPlugin | Memory store |
| VectorStorePlugin | Vector DB store/retrieve |
| ModelPlugin | LLM invocation |
| MCPPlugin | MCP tools |
| ToolPlugin | Tool execution |
| FilterPlugin | Filtering/ingestion |
| GuardrailPlugin | Guardrails |
| RefinementPlugin | Output refinement |
| EvaluationPlugin | Score/measure model output (quality gates) |
| FeedbackPlugin | Collect user feedback (training signal) |
| LearningPlugin | Incremental learning / model update |
| DatasetBuildPlugin | Build/curate dataset from feedback and evaluations |
| TrainTriggerPlugin | Trigger training job when conditions are met |
| ModelRegistryPlugin | Register or promote trained model for serving |
| ConditionPlugin | Group if/elseif/else: reads context, returns output key `branch` (0=then, 1=elseif, …, n-1=else). |
| PromptBuilderPlugin | Prompt building |
| ObservabilityPlugin | Observability |
| TracingPlugin | Tracing |
| BillingPlugin | Billing |
| FeatureFlagPlugin | Feature flags |
| AuditPlugin | Audit |
| SecurityScannerPlugin | Security scanning |
| CachingPlugin | Caching |
| SearchPlugin | Search |
| LangChainAdapterPlugin | LangChain adapter |
| AgentOrchestratorPlugin | Agent orchestration |
| WorkflowExtensionPlugin | Workflow extension |
| CustomStagePlugin | Custom stage |

Your implementation can use any of these that fits; the engine does not restrict which Java class implements which type. Keeping `pluginType` aligned with behavior helps operators and UIs.

---

## 8. Optional contracts (extensions)

### 8.0 Version compatibility check

The contract module exposes a **runtime version** and an optional **compatibility check** so the worker can reject plugins built for an incompatible contract version.

- **`ContractVersion.getCurrent()`** — Returns the current contract version (e.g. `"0.0.1"`), read from the published JAR’s `contract-version.properties` at build time.
- **`ContractCompatibility`** — Optional interface: implement **`getRequiredContractVersion()`** and return the contract version your plugin was built for (should match the `plugin-contract` dependency version you compile against).
- **`ContractVersion.isCompatible(requiredByPlugin, current)`** — Returns true if the plugin’s required version is compatible with the runtime contract (same major version; current ≥ required by semver).
- **`ContractVersion.requireCompatible(StageHandler)`** — If the handler implements `ContractCompatibility`, checks compatibility and throws **`ContractVersionException`** when incompatible.

The worker calls **`ContractVersion.requireCompatible(handler)`** before invoking a plugin. Plugins that do **not** implement `ContractCompatibility` are always considered compatible. Implement it to ensure your plugin is only run when the worker’s contract is compatible.

**Example (in your plugin):**

```java
public final class MyPlugin implements StageHandler, ContractCompatibility {
    private static final String CONTRACT_VERSION = "0.0.1"; // match plugin-contract dependency

    @Override
    public String getRequiredContractVersion() {
        return CONTRACT_VERSION;
    }
    // ... name(), execute(PluginContext)
}
```

### 8.1 StreamingStageHandler

When the **STREAMING** feature is enabled, the kernel may call a handler that implements `StreamingStageHandler` with a non-null `StreamObserver` for streaming output (tokens, intermediate updates).

```java
public interface StreamingStageHandler extends StageHandler {

    StageResult execute(ExecutionContext context, StreamObserver observer);

    @Override
    default StageResult execute(ExecutionContext context) {
        return execute(context, null);
    }
}
```

- If `observer == null`, behave like a normal `StageHandler`.
- If `observer != null`, call `observer.onToken(...)`, `observer.onUpdate(type, payload)`, then `observer.onComplete()` or `observer.onError(t)`.

`StreamObserver`:

- `onToken(String token)` — partial token or text chunk.
- `onUpdate(String type, Object payload)` — structured update (e.g. tool progress).
- `onComplete()` — stream ended successfully.
- `onError(Throwable t)` — stream ended with error.

### 8.2 CheckpointableStage

When the run is in replay or branch mode, the kernel may call checkpointable stages to resume or branch from a step.

```java
public interface CheckpointableStage extends StageHandler {

    boolean supportsReplay();

    StageResult resumeFrom(long stepId, ExecutionContext context);

    StageResult branchFrom(long stepId, ExecutionContext context);
}
```

Implement this only if your stage can safely resume or branch from a given step (e.g. deterministic, or with stored state).

### 8.3 OutputContract

When the **OUTPUT_CONTRACT** feature is enabled and a validator is registered, the kernel can validate your stage output against a schema.

```java
public interface OutputContract {

    /** Schema for this stage's output (e.g. JSON Schema). */
    Object getSchema();

    /** If true, validation failure leads to failure or retry. */
    boolean enforceStrict();
}
```

Implement `OutputContract` in addition to `StageHandler` if you want the engine to validate `getCurrentPluginOutput()` (and returned result) against `getSchema()`. Interpretation of the schema depends on the registered `OutputContractValidator`.

### 8.4 PlannerInputDescriptor (required fields for planner phase LL query)

When the **PLANNER** stage runs (e.g. an LLM that produces a dynamic plan), the worker needs to know which input keys to pull from `originalInput` and `accumulatedOutput` so the planner’s LL (language model) query has the right context. Implement **`PlannerInputDescriptor`** so your plugin declares the fields it reads; the worker then collects the **union** of these sets across all compatible plugins and uses it to render the data input for the planner phase.

```java
public interface PlannerInputDescriptor {

    /** Keys from originalInput/accumulatedOutput this plugin reads. Union is used for planner LL query data input. */
    Set<String> getRequiredInputFieldsForPlanner();

    /** Optional one-line description for the planner (e.g. for LLM system prompt or tool list). */
    default String getPlannerDescription() { return null; }
}
```

- **`getRequiredInputFieldsForPlanner()`** — Return the key names (e.g. `"question"`, `"document"`, `"modelId"`, `"messages"`) that your plugin reads from context. The worker aggregates these across all plugins and includes them in the planner’s data payload so the planner LLM can make informed decisions.
- **`getPlannerDescription()`** — Optional short description (e.g. “Vector retrieval; needs question and optional filters”) for inclusion in planner prompts or tool lists.

**Helpers on the contract:** `PlannerInputDescriptor.collectRequiredFields(handlers)` returns the union of all required field keys. `PlannerInputDescriptor.collectPluginInfo(handlers)` returns a list of `PlannerPluginInfo` (plugin name, required fields, description, optional type). **`PlannerInputDescriptor.collectAvailableTools(handlers, typesToInclude)`** returns only plugins whose type is in `typesToInclude` (e.g. `Set.of(PluginTypes.TOOL)`), so you can send "available tools" to the planner LLM; see §8.5.

Plugins that do not implement `PlannerInputDescriptor` are skipped when aggregating; no default is assumed.

**Example:**
```java
public final class MyRetrievalPlugin implements StageHandler, PlannerInputDescriptor {

    @Override
    public Set<String> getRequiredInputFieldsForPlanner() {
        return Set.of("question", "filters", "topK");
    }

    @Override
    public String getPlannerDescription() {
        return "Vector retrieval: needs question, optional filters and topK.";
    }
    // ... name(), execute(PluginContext)
}
```

### 8.5 PluginTypeDescriptor (available tools by type)

Implement **`PluginTypeDescriptor`** so the worker can **check the plugin type** and send only matching plugins as **available tools** to the planner. Use constants from **`PluginTypes`** (e.g. `PluginTypes.TOOL`, `PluginTypes.MODEL`) so types align with pipeline config `pluginType`.

```java
public interface PluginTypeDescriptor {

    /** The plugin type (e.g. PluginTypes.TOOL, PluginTypes.MODEL). */
    String getPluginType();
}
```

When building the planner's "available tools" list, call **`PlannerInputDescriptor.collectAvailableTools(handlers, typesToInclude)`**: it returns only handlers that implement both `PlannerInputDescriptor` and `PluginTypeDescriptor` and whose `getPluginType()` is in `typesToInclude`. For example, to send only tool plugins:

```java
List<PlannerPluginInfo> availableTools = PlannerInputDescriptor.collectAvailableTools(
    compatibleHandlers,
    Set.of(PluginTypes.TOOL)
);
// Render availableTools as the planner LLM's tool list (name, required fields, description).
```

If `typesToInclude` is null or empty, all handlers that implement `PlannerInputDescriptor` are included (no type filter). `PlannerPluginInfo` includes an optional `pluginType` field when the handler implements `PluginTypeDescriptor`.

---

## 9. Merge policies (ASYNC groups)

For ASYNC groups, the engine runs multiple activities in parallel and then merges their outputs. Two mechanisms:

### 9.1 Merge policy name (asyncOutputMergePolicy)

In pipeline config you can set `asyncOutputMergePolicy` to a **name** (e.g. `LAST_WINS`, `FIRST_WINS`, `PREFIX_BY_ACTIVITY`). The engine uses a built-in or config-registered policy to merge results into the accumulated map. No plugin code required for these.

### 9.2 Merge handler as a plugin (mergePolicy hook)

You can run a **merge plugin** after an ASYNC group. The plugin is a normal `StageHandler` registered by name. The engine invokes it with an execution context where:

- `getAccumulatedOutput()` reflects state before the merge.
- Context state can contain `asyncStageResults` (list of activity name + result) so the plugin can merge them and write the result via `putOutput(...)`.

Example: `LastWinsMergeHandler` implements `StageHandler`, reads `asyncStageResults` from context, merges, and writes to `currentPluginOutput`. It is registered under the name `LAST_WINS` and can be referenced in config as the merge policy or as a merge hook.

### 9.3 AsyncMergePolicy (programmatic)

For custom merge logic you can implement `AsyncMergePolicy` and register it by name in `MergePolicyRegistry` (or via config that instantiates the class). This is a different interface from `StageHandler`; it is used by the kernel to merge a list of named results into the accumulated map. See engine package `com.openllmorchestrator.worker.engine.kernel.merge`.

---

## 10. Conventions and best practices

### 10.1 Output keys

- Use clear, stable key names (e.g. `retrievedChunks`, `result`, `response`, `output`, `tokenizedChunks`). Downstream stages and pipelines depend on these.
- Prefer a small, documented set of keys per plugin so pipelines are predictable.

### 10.2 Errors

- Throw a **runtime exception** to fail the activity. The engine and Temporal will apply retries according to the activity retry policy. Use `nonRetryableErrors` in config for exceptions that must not be retried.
- Do not swallow errors and return an empty result unless “skip and continue” is intended; otherwise the workflow may assume success and continue with missing data.

### 10.3 Idempotency

- Activities may be retried. Prefer idempotent behavior (e.g. same input → same output, or safe to re-run) so retries do not cause duplicate side effects.

### 10.4 Statelessness

- Do not keep mutable state in the handler across `execute()` calls unless you explicitly manage it and understand worker lifecycle. Prefer reading everything from `ExecutionContext` and writing only to `putOutput` and `StageResult`.

### 10.5 Name stability

- Your `name()` and the registration key (and config `name`) must be the same and stable across versions if you want existing pipelines to keep resolving to your plugin.

---

## 11. Minimal plugin example

Below is a minimal plugin that reads from accumulated output and original input, writes one key, and returns a result. It implements only `StageHandler`.

```java
package com.example.myplugin;

import com.openllmorchestrator.worker.engine.contract.ExecutionContext;
import com.openllmorchestrator.worker.engine.contract.StageResult;
import com.openllmorchestrator.worker.engine.stage.StageHandler;

import java.util.HashMap;
import java.util.Map;

public final class MyPlugin implements StageHandler {

    public static final String NAME = "com.example.myplugin.MyPlugin";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public StageResult execute(ExecutionContext context) {
        Map<String, Object> input = context.getOriginalInput();
        Map<String, Object> accumulated = context.getAccumulatedOutput();

        // Read
        String question = (String) input.get("question");
        Object previous = accumulated.get("someKey");

        // Compute and write
        String answer = compute(question, previous);
        context.putOutput("result", answer);

        return StageResult.builder()
                .stageName(NAME)
                .output(new HashMap<>(context.getCurrentPluginOutput()))
                .build();
    }

    private String compute(String question, Object previous) {
        return "computed from " + question;
    }
}
```

**Registration (built-in):** In your bootstrap step, register with the same name:

```java
builder.register(MyPlugin.NAME, new MyPlugin());
```

**Pipeline config:**

```json
{
  "type": "STAGE",
  "pluginType": "CustomStagePlugin",
  "name": "com.example.myplugin.MyPlugin"
}
```

---

## 12. Dynamic JAR example

To ship the same plugin in a JAR:

1. **JAR layout:**
   - Your class: `com.example.myplugin.MyPlugin`
   - Service file: `META-INF/services/com.openllmorchestrator.worker.engine.stage.StageHandler`  
     Content: `com.example.myplugin.MyPlugin` (single line, no extra newline if you want to avoid empty second provider)
   - Dependencies: bundle or use a fat JAR so the worker can load your class.

2. **Config:**
   ```json
   "dynamicPlugins": {
     "com.example.myplugin.MyPlugin": "/opt/plugins/my-plugin.jar"
   }
   ```

3. **Pipeline:** Use `"name": "com.example.myplugin.MyPlugin"` as in the example above.

Your plugin’s `name()` should return `"com.example.myplugin.MyPlugin"` so it matches the config key and pipeline.

---

## 13. Summary checklist

Use this when implementing a new plugin:

- [ ] Implement `StageHandler`: `name()` and `execute(ExecutionContext)`.
- [ ] Use only `getOriginalInput()`, `getAccumulatedOutput()`, and `putOutput()` / `getCurrentPluginOutput()` for I/O; do not retain context after return.
- [ ] Return a `StageResult` with at least `stageName` and `output` (or equivalent); set `requestPipelineBreak` if needed.
- [ ] Register the handler under the **exact** name that pipeline config will use (`name` in STAGE node).
- [ ] Choose an allowed `pluginType` for the STAGE node and keep it consistent with behavior.
- [ ] (Optional) Implement `StreamingStageHandler`, `CheckpointableStage`, or `OutputContract` if you need those features.
- [ ] (Optional) For dynamic loading, add the JAR with the correct ServiceLoader file and register the plugin name in `dynamicPlugins`.

This contract is the single reference for the community to build and scale plugins against the Open LLM Orchestrator Worker.
