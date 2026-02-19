# Open LLM Orchestrator — Full Feature List

A single reference for **all features, qualities, use cases, and community offerings** of the Open LLM Orchestrator Worker and its ecosystem: config-driven pipelines, Redis-based configuration, drag-and-drop UI, Docker one-click stack, chat application, and ready-made templates.

---

## 1. Core qualities

| Quality | Description |
|--------|-------------|
| **Config-first** | Pipelines, timeouts, retries, merge policies, task queue, and capability order come from configuration (file, Redis, or DB). No hardcoded flows in the engine. |
| **Temporal-native** | Single workflow type (`CoreWorkflowImpl`); activities per capability; workflow returns full result map (including LLM `result`). Durable, observable, retryable. |
| **Extensible** | Add pipelines and plugins by config and registration; async merge is a plugin invoked as an activity; custom capabilities and plugin types supported. |
| **Redis-backed config** | Config loaded in order **Redis → DB → file**. Config can be pushed at runtime to Redis; worker and UI share the same config source. |
| **Runtime config push** | Drag-and-drop UI (or API) pushes pipeline configuration to Redis at runtime; workers pick up config on next load or refresh. |
| **Template-driven** | Ready-made pipeline templates (text, image, video, multi-model, planner) selectable from a dropdown; one selection switches the active use case. |
| **Community-ready stack** | One-click Docker Compose stack with sample configuration so the community can start experimenting with minimal setup. |

---

## 2. Use cases covered

| Use case | Description | Pipeline / template |
|----------|-------------|----------------------|
| **Text chat** | Single-model chat (no RAG). Input: question or messages. | `chat-*` (e.g. chat-mistral, chat-llama3.2). |
| **RAG (retrieve + LLM)** | Retrieve chunks for a question, then LLM answer. | `question-answer`, `rag-*` (e.g. rag-mistral, rag-llama3.2). |
| **Document ingestion** | Tokenize document and store chunks in vector store. | `document-ingestion`. |
| **Folder ingestion** | Read all files from a folder, tokenize, store chunks. | `document-ingestion-folder`. |
| **Multi-model query** | Query multiple models in parallel; merged output per model. | `query-all-models`. |
| **Image** | Pipelines and templates for image inputs/outputs. | Image template (dropdown). |
| **Video** | Pipelines and templates for video inputs/outputs. | Video template (dropdown). |
| **Planner** | LLM-driven plan (task decomposition); PLAN_EXECUTOR runs sub-plan. | Planner configuration template (dropdown). |
| **Custom / learning** | Custom capabilities, learning pipelines, feedback, dataset build, train trigger. | Configurable via capabilityOrder and pipelines. |

---

## 3. Configuration and pipeline model

| Feature | Description |
|--------|-------------|
| **Pipeline definition** | Recursive GROUP/STAGE (or PLUGIN) trees, or capability-based flows with SYNC/ASYNC groups. Each capability implemented by a plugin (activity) registered by name. |
| **Capability order** | Fixed flow via `capabilityOrder` (or `stageOrder`); only capabilities present in pipeline root are run, in that order. |
| **Custom capabilities** | Define in config (`capabilities`: name → `{ "pluginType", "name" }`); reference anywhere in the flow. |
| **Group rules** | Each group: at most one **PLUGIN_IF** (conditional), at most one **PLUGIN_ITERATOR** (iterative). SYNC = sequential; ASYNC = one FORK + one JOIN (from group or engine default). |
| **Condition flow** | Group-level if/elseif/else: condition plugin writes `branch` (0=then, 1=elseif, …, n−1=else); one branch runs. |
| **Merge policies** | Async merge: built-in (FIRST_WINS, LAST_WINS, PREFIX_BY_ACTIVITY) or custom plugin; configurable per pipeline/group. |
| **Load order** | Redis → DB → file. File path: `config/<CONFIG_KEY>.json`. When loaded from file, config is written back to Redis and DB. |
| **Environment overrides** | Production: queue name, Redis, DB, Temporal, Ollama URLs overridden by env vars (e.g. `CONFIG_KEY`, `REDIS_HOST`, `DB_URL`, `OLLAMA_BASE_URL`). |

---

## 4. Drag-and-drop pipeline configuration

| Feature | Description |
|--------|-------------|
| **Drag-and-drop UI** | Visual pipeline editor: build flows by dragging capabilities and groups; drop to reorder or nest. |
| **Redis-based config** | Configuration produced by the UI is pushed to Redis (key `olo:engine:config:<CONFIG_KEY>:<version>`); workers and other services use the same config. |
| **Runtime push** | Push configuration at runtime from the UI; no worker restart required when config is loaded from Redis (or on next config refresh). |
| **Ready-made templates** | Dropdown to select a template; switching template applies a full pipeline configuration (text, image, video, multiple model, planner). |
| **Template types** | **Text** — single-model chat / RAG. **Image** — image input/output pipelines. **Video** — video pipelines. **Multiple model** — parallel model query. **Planner** — planner + plan executor configuration. |
| **Validation** | UI and engine validate: capability names, plugin types, at most one PLUGIN_IF and one PLUGIN_ITERATOR per group, SYNC/ASYNC, resolvable plugins. |

Reference for building the UI: [ui-reference.md](ui-reference.md), [configuration-reference.md](configuration-reference.md).

---

## 5. Chat application

| Feature | Description |
|--------|-------------|
| **Included chat app** | A small chat application is part of the Docker stack; users can interact with the orchestrator via chat from day one. |
| **First-engagement** | Only delay for the user is image pull/startup (e.g. Docker images); once the stack is up, the user can open the chat and start the first conversation immediately. |
| **Pipeline-backed** | Chat uses config-driven pipelines (e.g. chat-*, question-answer, RAG); switching template in the UI can change the chat behavior (single model, RAG, multi-model, etc.). |

---

## 6. Docker and one-click stack

| Feature | Description |
|--------|-------------|
| **One-click Docker stack** | Single script / `docker compose` to start all relevant containers with sample configuration. |
| **Docker Compose** | Starts worker, Temporal, Redis, Postgres, Ollama, and any UI/chat services; wired with correct env (Temporal target, Ollama URL, Redis, DB). |
| **Sample configuration** | Stack includes sample engine config so the community can play without editing files; config can be file-based or preloaded to Redis. |
| **Template dropdown** | Users toggle use cases by selecting a template (text, image, video, multi-model, planner) in the UI; selection drives which pipeline/config is active. |
| **Image download delay** | The only significant delay for first run is pulling Docker images; after that, the user can engage with chat and pipelines immediately. |

---

## 7. Feature flags and optional behavior

| Flag / area | Description |
|-------------|-------------|
| **Feature flags** (`enabledFeatures`) | HUMAN_SIGNAL, STREAMING, AGENT_CONTEXT, DETERMINISM_POLICY, CHECKPOINTABLE_STAGE, OUTPUT_CONTRACT, EXECUTION_GRAPH, STAGE_RESULT_ENVELOPE, VERSIONED_STATE, INTERCEPTORS, PLANNER_PLAN_EXECUTOR, EXECUTION_SNAPSHOT, POLICY_ENGINE, BUDGET_GUARDRAIL, CONCURRENCY_ISOLATION, SECURITY_HARDENING, PLAN_SAFETY_VALIDATION, EXECUTION_GRAPH_EXPORT. Only listed features execute. |
| **Plugin types** | AccessControlPlugin, ModelPlugin, VectorStorePlugin, ToolPlugin, MCPPlugin, FilterPlugin, ConditionPlugin, IteratorPlugin, ForkPlugin, JoinPlugin, MergePolicy, etc. See [ui-reference.md](ui-reference.md). |
| **Dynamic plugins** | JAR paths per plugin name or multi-handler JARs; loaded at bootstrap; registered by activity name. |

---

## 8. Engine config module and builder API

| Feature | Description |
|--------|-------------|
| **engine-config module** | Separate library for config POJOs, serialization, and persistence; same schema for worker and any client (CLI, UI, config service). |
| **Builder API** | Fluent builders for EngineFileConfig, pipelines, nodes, groups; build config in code then serialize to JSON. |
| **Write to file or Redis** | `EngineConfigWriter`: write config to local JSON file or to Redis (engine key or queue key); clients can push config without touching the worker. |
| **Templates in code** | Use `EngineConfigBuilders` and builders to generate template configs (text, image, video, multi-model, planner) programmatically. |

See [engine-config README](../engine-config/README.md).

---

## 9. Summary table: what’s in the box

| Item | Included |
|------|----------|
| Config-driven pipelines | Yes (file, Redis, DB) |
| Drag-and-drop pipeline UI | Yes (Redis-based, runtime push) |
| Ready-made templates | Yes (text, image, video, multi-model, planner; dropdown) |
| One-click Docker stack | Yes (Compose + sample config) |
| Chat application | Yes (in stack; first engagement after image pull) |
| RAG, document ingestion, multi-model | Yes (pipelines + templates) |
| Group rules (PLUGIN_IF, PLUGIN_ITERATOR, FORK/JOIN) | Yes (validated; engine defaults) |
| Feature flags, plugin types, merge policies | Yes (configurable) |
| engine-config library (build, serialize, write to Redis/file) | Yes |

---

## 10. Quick links

| Doc | Purpose |
|-----|--------|
| [README](../README.md) | Project overview, quick start, Docker. |
| [config/README](../config/README.md) | Config location, examples, UI design. |
| [configuration-reference.md](configuration-reference.md) | Full config schema, validation, drag-and-drop guidance. |
| [ui-reference.md](ui-reference.md) | Predefined capabilities, plugin types, pipeline structure for UI. |
| [config-reference.md](config-reference.md) | Load order, env vars, Redis key. |
| [engine-config README](../engine-config/README.md) | Build config in code, write to file/Redis. |
