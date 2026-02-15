<!-- Copyright 2026 Open LLM Orchestrator contributors. Licensed under the Apache License, Version 2.0; see LICENSE file. -->

# Open LLM Orchestrator Worker — Architecture

High-level architecture and package layout for the worker engine.

---

## 1. System Context

```mermaid
flowchart LR
    subgraph External["External"]
        Temporal[Temporal Server]
        ConfigFile["config/engine-config.json"]
    end

    subgraph Worker["Worker Process"]
        App[WorkerApplication]
        WF[CoreWorkflow]
        Act[KernelStageActivity]
    end

    ConfigFile -->|"1. Load once at startup"| App
    App -->|"2. Bootstrap"| WF
    Temporal -->|"3. Dispatch workflows"| WF
    WF -->|"4. Invoke activities"| Act
    Act -->|"5. Run stages"| Act
```

- **Temporal** drives workflows and activities.
- **Config file** is loaded once at bootstrap; pipeline is recursive (GROUP/STAGE tree).
- **Worker** runs workflows and activities on the configured task queue.

---

## 2. High-Level Components

```mermaid
flowchart TB
    subgraph Entry["Entry"]
        WorkerApp[WorkerApplication]
    end

    subgraph Bootstrap["Bootstrap"]
        WB[WorkerBootstrap]
        Steps[LoadConfig · BuildResolver · Validate · BuildPlan · SetRuntime]
    end

    subgraph Config["Config"]
        Loader[config.loader]
        Validation[config.validation]
        DTOs[worker · redis · database · pipeline]
    end

    subgraph Stage["Stage Engine"]
        Plan[stage.plan]
        Predefined[stage.predefined]
        Custom[stage.custom]
        Resolver[stage.resolver]
        Bucket[stage.bucket]
        Handlers[stage.handler]
    end

    subgraph Kernel["Kernel"]
        Orch[KernelOrchestrator]
        Invoker[StageInvoker]
        Exec[kernel.execution]
    end

    subgraph Runtime["Runtime"]
        EngineRuntime[EngineRuntime]
    end

    subgraph TemporalBoundary["Temporal Boundary"]
        Workflow[CoreWorkflow]
        Activity[KernelStageActivity]
    end

    WorkerApp --> WB
    WB --> Steps
    Steps --> Loader
    Steps --> Validation
    Steps --> Plan
    Steps --> Resolver
    Steps --> EngineRuntime

    Loader --> DTOs
    Validation --> DTOs
    Plan --> DTOs
    Resolver --> Predefined
    Resolver --> Custom
    Bucket --> Predefined
    Bucket --> Custom
    Bucket --> Handlers

    Workflow --> EngineRuntime
    Workflow --> Orch
    Orch --> Exec
    Orch --> Invoker
    Activity --> EngineRuntime
    Activity --> Resolver
```

---

## 3. Package Structure (Small Packages, Single Responsibility)

```mermaid
flowchart LR
    subgraph config["engine.config"]
        config_root[EngineFileConfig]
        subgraph config_loader["config.loader / config.source"]
            HierarchicalLoader[HierarchicalConfigLoader]
            FileRepo[FileConfigRepository]
        end
        subgraph config_validation["config.validation"]
            Validators[ConfigValidator + 5 impls]
        end
        subgraph config_dto["config sections"]
            worker[worker.WorkerConfig]
            redis[redis.RedisConfig]
            db[database.DatabaseConfig]
            pipe[pipeline.PipelineSection, NodeConfig]
        end
    end

    subgraph bootstrap["engine.bootstrap"]
        BootstrapCtx[BootstrapContext]
        BootstrapStep[BootstrapStep]
        WorkerBootstrap[WorkerBootstrap]
        subgraph steps["bootstrap.steps"]
            S1[LoadConfigStep]
            S2[BuildResolverStep]
            S3[ValidateConfigStep]
            S4[BuildPlanStep]
            S5[SetRuntimeStep]
        end
    end

    subgraph stage["engine.stage"]
        Handler[StageHandler]
        Definition[StageDefinition]
        Plan[StagePlan]
        PlanBuilder[StagePlanBuilder]
        subgraph plan["stage.plan"]
            PlanFactory[StagePlanFactory]
            NodeProcs[NodeProcessor, StageNode, GroupNode]
        end
        subgraph predefined["stage.predefined"]
            PredefStages[PredefinedStages]
            PredefBucket[PredefinedPluginBucket]
        end
        subgraph custom["stage.custom"]
            CustomBucket[CustomStageBucket]
        end
        subgraph resolver["stage.resolver"]
            StageResolver[StageResolver]
        end
        subgraph bucket["stage.bucket"]
            BucketFactory[StageBucketFactory]
        end
        subgraph handler["stage.handler"]
            Access[AccessStageHandler]
            Memory[MemoryStageHandler]
            Retrieval[RetrievalStageHandler]
            Model[ModelStageHandler]
        end
    end

    subgraph kernel["engine.kernel"]
        Orchestrator[KernelOrchestrator]
        Invoker[StageInvoker]
        subgraph execution["kernel.execution"]
            GroupExec[GroupExecutor]
            SyncExec[SyncGroupExecutor]
            AsyncExec[AsyncGroupExecutor]
        end
    end

    subgraph other["Other"]
        runtime[engine.runtime.EngineRuntime]
        contract[engine.contract]
        activity[engine.activity]
        workflow[workflow]
    end
```

---

## 4. Bootstrap Flow (One-Time)

```mermaid
sequenceDiagram
    participant App as WorkerApplication
    participant WB as WorkerBootstrap
    participant Load as LoadConfigStep
    participant BuildR as BuildResolverStep
    participant Valid as ValidateConfigStep
    participant BuildP as BuildPlanStep
    participant SetR as SetRuntimeStep
    participant Loader as HierarchicalConfigLoader
    participant Runtime as EngineRuntime

    App->>WB: initialize()
    WB->>WB: Create predefined + custom buckets

    WB->>Load: run(ctx)
    Load->>Loader: HierarchicalConfigLoader (Redis → DB → file)
    Loader-->>Load: EngineFileConfig
    Load->>Runtime: ctx.setConfig(config)

    WB->>BuildR: run(ctx)
    BuildR->>Runtime: ctx.setResolver(StageResolver)

    WB->>Valid: run(ctx)
    Valid->>Valid: EngineConfigValidator.validate()
    Note over Valid: NotNull, Worker, PipelineRoot, PipelineNode

    WB->>BuildP: run(ctx)
    BuildP->>Runtime: ctx.setPlans(plans from pipelines), ctx.setPlan if "default"

    WB->>SetR: run(ctx)
    SetR->>Runtime: setStageResolver, setConfig, setStagePlan

    WB-->>App: config
```

---

## 5. Workflow Execution Flow (Per Run)

```mermaid
sequenceDiagram
    participant Temporal
    participant Workflow as CoreWorkflowImpl
    participant Runtime as EngineRuntime
    participant Orch as KernelOrchestrator
    participant Exec as Sync/AsyncGroupExecutor
    participant Invoker as StageInvoker
    participant Activity as KernelStageActivityImpl
    participant Resolver as StageResolver
    participant Handler as StageHandler

    Temporal->>Workflow: execute(ExecutionCommand)
    Workflow->>Runtime: getStagePlan()
    Runtime-->>Workflow: StagePlan

    loop For each group in plan
        Workflow->>Orch: execute(plan, context)
        Orch->>Exec: supports(group)? execute(group, invoker, context)

        alt SYNC group
            loop For each stage
                Exec->>Invoker: invokeSync(definition)
                Invoker->>Activity: execute(stageName)
                Activity->>Resolver: resolve(stageName)
                Resolver-->>Activity: StageHandler
                Activity->>Handler: execute(context)
                Handler-->>Activity: StageResult
            end
        else ASYNC group
            Exec->>Invoker: invokeAsync(definition) x N
            Invoker->>Activity: execute(stageName) x N
            Activity->>Resolver: resolve(stageName)
            Resolver-->>Activity: StageHandler
            Activity->>Handler: execute(context)
            Note over Activity: Promise.allOf(...)
        end
    end
```

---

## 6. Stage Resolution (Predefined vs Custom)

```mermaid
flowchart LR
    subgraph Input["Input"]
        Name[stageName]
    end

    subgraph Resolver["StageResolver"]
        Predefined{"Predefined?"<br/>ACCESS,MEMORY,<br/>RETRIEVAL,MODEL}
    end

    subgraph PredefinedPath["Predefined path"]
        Config["config (optional stagePlugins)"]
        PredefBucket[PredefinedPluginBucket]
        Plugin["plugin id → handler"]
    end

    subgraph CustomPath["Custom path"]
        CustomBucket[CustomStageBucket]
        CustomLookup["stage name → handler"]
    end

    Name --> Predefined
    Predefined -->|Yes| Config
    Config --> PredefBucket
    PredefBucket --> Plugin
    Predefined -->|No| CustomBucket
    CustomBucket --> CustomLookup
    Plugin --> Handler[StageHandler]
    CustomLookup --> Handler
```

- **Predefined stages** (ACCESS, MEMORY, RETRIEVAL, MODEL): plugin id from config (optional `stagePlugins`) or default, then lookup in `PredefinedPluginBucket`.
- **Custom stages**: any other name is resolved only from `CustomStageBucket`. If missing, the stage fails with a clear error.

---

## 7. Pipeline Config Shape (Recursive)

```mermaid
flowchart TD
    Root["root: GROUP (SYNC)"]
    Root --> A["ACCESS (STAGE)"]
    Root --> G["GROUP (ASYNC)"]
    Root --> M["MODEL (STAGE)"]
    G --> Mem["MEMORY (STAGE)"]
    G --> Ret["RETRIEVAL (STAGE)"]

    style Root fill:#e1f5fe
    style G fill:#fff3e0
    style A fill:#e8f5e9
    style Mem fill:#e8f5e9
    style Ret fill:#e8f5e9
    style M fill:#e8f5e9
```

- **GROUP** = container; `executionMode`: SYNC (sequential) or ASYNC (parallel).
- **STAGE** = leaf; `name` must match a predefined stage (with plugin in config) or a custom stage (registered in custom bucket).

---

## 8. Design Principles

| Principle | How it’s applied |
|-----------|-------------------|
| **Small packages** | One clear responsibility per package (e.g. `config.loader`, `config.validation`, `stage.plan`, `stage.resolver`). |
| **Open/Closed** | New behaviour via new classes: new `ConfigValidator`, `NodeProcessor`, `GroupExecutor`, `BootstrapStep`; existing code unchanged. |
| **No reflection** | Stages and plugins are registered at bootstrap; resolution is map lookup and direct invocation. |
| **One-time config** | Config loaded once from file; recursive validation; plan and resolver built once and stored in `EngineRuntime`. |
| **Pluggable stages** | Predefined stages use config-driven plugin id; custom stages use a separate bucket; both fail clearly if unresolved. |

---

## 9. Key Files Quick Reference

| Concern | Location |
|--------|----------|
| Config file path | `config/engine-config.json` or `-Dengine.config.path` |
| Root config | `engine.config.EngineFileConfig` |
| Load config | `engine.config.loader.HierarchicalConfigLoader` (sources: Redis, DB, file) |
| Validate config | `engine.config.validation.EngineConfigValidator` |
| Build plan from pipeline | `engine.stage.plan.StagePlanFactory` |
| Resolve stage → handler | `engine.stage.resolver.StageResolver` |
| Default stage handlers | `engine.stage.handler.*` |
| Bootstrap entry | `engine.bootstrap.WorkerBootstrap` |
| Runtime state | `engine.runtime.EngineRuntime` |
| Workflow entry | `workflow.impl.CoreWorkflowImpl` |
| Activity entry | `engine.activity.impl.KernelStageActivityImpl` |
