# Dynamic execution plan: PLANNER and PLAN_EXECUTOR

This document describes how a **dynamic execution tree** is created at the **PLANNER** capability and executed under **PLAN_EXECUTOR**. The dynamic plan is the only execution tree that is created at **runtime**; all other plans are built at **bootstrap** from config.

---

## 1. Overview

| Plan type   | When created     | Where used                    | Scope                          |
|------------|------------------|-------------------------------|--------------------------------|
| **Static** | Bootstrap        | Workflow selects a pipeline   | Entire run (e.g. `default`)    |
| **Dynamic**| Runtime (PLANNER)| Kernel when it hits PLAN_EXECUTOR | Within PLAN_EXECUTOR only |

- The **static** plan comes from `EngineRuntime.getCapabilityPlan(pipelineName)` and is built once at startup from `pipelines.<name>` in config.
- The **dynamic** plan is produced by a **PLANNER** plugin (e.g. an LLM) during the run, stored in execution context, and executed only when the kernel reaches the **PLAN_EXECUTOR** capability. The execution tree for that plan is created and run **only within the scope** of PLAN_EXECUTOR (same kernel, same context, same interceptors).

---

## 2. Creating the dynamic tree at PLANNER

### 2.1 Who runs PLANNER

The **PLANNER** capability is implemented by a **plugin** (e.g. `PlannerCapabilityHandler` or a custom LLM-based planner). That plugin runs as a normal capability: it receives `ExecutionContext` (original input, accumulated output from previous capabilities) and writes its output via `context.putOutput(...)`.

### 2.2 Building a `CapabilityPlan`

The plugin must build a `CapabilityPlan` and store it in context. A `CapabilityPlan` is an **ordered list of capability groups**; each group is a `CapabilityGroupSpec` (e.g. one or more capability definitions, SYNC or ASYNC, optional merge policy).

**Option A: Programmatic build with `CapabilityPlanBuilder`**

```java
CapabilityPlanBuilder builder = CapabilityPlan.builder();

// Add a SYNC group (one capability)
builder.addSyncWithCustomConfig(
    "RETRIEVAL",                           // capability/plugin name (activity id)
    CapabilityExecutionMode.SYNC,
    Duration.ofSeconds(60),
    taskQueue,
    null, null, null, "RETRIEVAL");

// Add an ASYNC group (multiple capabilities in parallel)
builder.addAsyncGroup(
    List.of("ToolPluginA", "ToolPluginB"),
    Duration.ofSeconds(30),
    taskQueue,
    null, null, null,
    AsyncCompletionPolicy.ALL,
    "LAST_WINS");

CapabilityPlan plan = builder.build();
```

**Option B: From a list of group specs**

```java
List<CapabilityGroupSpec> groups = ...;  // build specs as needed
CapabilityPlan plan = CapabilityPlan.fromGroups(groups);
```

Only capability/plugin **names** that are **resolvable** at runtime (registered in the activity registry or custom bucket) should be used. The kernel will resolve them when it executes the dynamic plan.

### 2.3 Storing the plan in context

The plugin **must** write the plan into the execution context under the well-known key so that PLAN_EXECUTOR can read it:

```java
context.putOutput(PlannerContextKeys.KEY_DYNAMIC_PLAN, plan);
```

- **Key:** `PlannerContextKeys.KEY_DYNAMIC_PLAN` = `"dynamicPlan"`.
- **Value:** A `CapabilityPlan` instance (same type as bootstrap-built plans).

After the PLANNER capability completes, the kernel merges this output into **accumulated output**. When the kernel later reaches the PLAN_EXECUTOR capability, it reads `accumulated.get("dynamicPlan")` and, if it is a `CapabilityPlan`, runs it.

### 2.4 Contract summary (PLANNER)

| Item        | Requirement |
|------------|-------------|
| Key        | `"dynamicPlan"` (`PlannerContextKeys.KEY_DYNAMIC_PLAN`) |
| Value type | `CapabilityPlan` |
| When       | Before the run reaches PLAN_EXECUTOR (PLANNER usually runs earlier in the static plan). |
| Resolution | Every capability name in the dynamic plan must be resolvable (activity registry or custom bucket). |

---

## 3. Executing the dynamic tree under PLAN_EXECUTOR

### 3.1 PLAN_EXECUTOR is not a plugin

PLAN_EXECUTOR is a **special capability**: the kernel does **not** invoke an activity for it. Instead, a dedicated **group executor** (`PlanExecutorGroupExecutor`) runs when the current group is the PLAN_EXECUTOR capability. That executor:

1. Reads the dynamic plan from context.
2. Optionally validates it (if `PLAN_SAFETY_VALIDATION` is enabled).
3. Runs the plan with the **same** kernel and **same** context.

So the **execution tree** for the dynamic plan is created and executed **only here**—within the scope of the PLAN_EXECUTOR node.

### 3.2 Execution flow inside PLAN_EXECUTOR

1. **Read plan:**  
   `context.getAccumulatedOutput().get(PlannerContextKeys.KEY_DYNAMIC_PLAN)`  
   If the value is not a `CapabilityPlan`, PLAN_EXECUTOR does nothing (no-op).

2. **Validate (optional):**  
   If the feature flag `PLAN_SAFETY_VALIDATION` is enabled, `EngineRuntime.getPlanValidator().validate(subPlan, context)` is called. If validation fails, execution throws and the run fails.

3. **Run the plan:**  
   The kernel’s `execute(plan, context)` is invoked with the **dynamic** plan and the **current** context. So:
   - Same **context**: same `originalInput`, same `accumulatedOutput` (including the dynamic plan and any prior capability outputs), same `versionedState`.
   - Same **kernel**: same group executors (PlanExecutor, Conditional, Sync, Async), same **interceptors** (beforeCapability, afterCapability, onError), same **feature flags**.

4. **Result:**  
   After the sub-plan completes, accumulated output and versioned state reflect all capability runs inside the dynamic plan. PLAN_EXECUTOR then reports success (or propagates an error) and the **static** plan continues with the next group, if any.

### 3.3 Scope guarantee

- **No separate bootstrap hierarchy** for dynamic content. The dynamic plan is a **runtime value** (a `CapabilityPlan` object) produced by PLANNER and consumed only by PLAN_EXECUTOR.
- **Same execution semantics:** Everything that applies to the static plan (interceptors, feature flags, versioned state, pipeline break, suspend-for-signal) applies to the dynamic plan run inside PLAN_EXECUTOR.

---

## 4. Sequence diagram

```mermaid
sequenceDiagram
    participant Workflow as CoreWorkflow
    participant Kernel as KernelOrchestrator
    participant StaticPlan as Static CapabilityPlan
    participant PLANNER as PLANNER plugin
    participant Ctx as ExecutionContext
    participant PLAN_EXEC as PlanExecutorGroupExecutor
    participant DynamicPlan as Dynamic CapabilityPlan

    Workflow->>Kernel: execute(staticPlan, context)
    Kernel->>StaticPlan: get next group (e.g. PLANNER)
    Kernel->>PLANNER: run PLANNER capability (activity)
    PLANNER->>PLANNER: build CapabilityPlan (e.g. via CapabilityPlanBuilder)
    PLANNER->>Ctx: putOutput("dynamicPlan", plan)
    Kernel->>Kernel: merge output; next group

    Kernel->>StaticPlan: get next group (PLAN_EXECUTOR)
    Kernel->>PLAN_EXEC: execute(PLAN_EXECUTOR spec, context)
    PLAN_EXEC->>Ctx: getAccumulatedOutput().get("dynamicPlan")
    Ctx-->>PLAN_EXEC: CapabilityPlan (dynamicPlan)
    opt PLAN_SAFETY_VALIDATION enabled
        PLAN_EXEC->>PLAN_EXEC: PlanValidator.validate(subPlan, context)
    end
    PLAN_EXEC->>Kernel: run(subPlan, context)  [same as kernel.execute(subPlan, context)]
    Kernel->>DynamicPlan: execute(dynamicPlan, context)
    loop For each group in dynamic plan
        Kernel->>Kernel: execute group (SYNC/ASYNC/Conditional)
        Note over Kernel: Same invoker, same interceptors, same context
    end
    Kernel-->>PLAN_EXEC: completed
    PLAN_EXEC-->>Kernel: done
    Kernel->>StaticPlan: next group (if any)
```

---

## 5. Feature flag and iterative use

- **Feature flag:** `PLANNER_PLAN_EXECUTOR` enables the use of PLANNER and PLAN_EXECUTOR in the static plan. When this flag is off, the static plan typically does not include these capabilities.
- **Iterative use:** When PLAN_EXECUTOR runs **inside an iterator** (e.g. inside ITERATIVE_BLOCK), the same flow applies: each time the kernel hits PLAN_EXECUTOR, it reads the **current** value of `dynamicPlan` from context and runs it. So an LLM can update the plan on each iteration and PLAN_EXECUTOR will run the latest plan with the current context.

---

## 6. Summary

| Phase        | Where        | What happens |
|-------------|--------------|--------------|
| **Creation**| PLANNER      | A plugin builds a `CapabilityPlan` (e.g. via `CapabilityPlanBuilder`) and stores it with `context.putOutput(PlannerContextKeys.KEY_DYNAMIC_PLAN, plan)`. |
| **Storage** | Context      | The plan lives in accumulated output under the key `"dynamicPlan"`. |
| **Execution** | PLAN_EXECUTOR | The kernel reads the plan from context, optionally validates it, then runs it with `kernel.execute(subPlan, context)`—same kernel, same context, same interceptors. The dynamic execution tree is run **only within this scope**. |

See also: [architecture.md §4.4 Dynamic execution tree only within PLAN_EXECUTOR scope](architecture.md#44-dynamic-execution-tree-only-within-plan_executor-scope).  
For bootstrap vs runtime plans: [architecture.md §4. Execution model](architecture.md#4-execution-model-acid-stateless-bootstrap-runtime-fairness-dynamic-tree).
