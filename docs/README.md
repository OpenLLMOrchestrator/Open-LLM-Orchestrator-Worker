# Documentation

Index of project documentation. Config loading and environment variables are documented in [config-reference.md](config-reference.md).

---

## Core docs

| Document | Description |
|----------|-------------|
| [**config-reference.md**](config-reference.md) | **Config reference:** Load order (Redis → DB → file), file path **`config/<CONFIG_KEY>.json`**, env vars (`CONFIG_KEY`, `CONFIG_VERSION`, `CONFIG_FILE_PATH`, Redis, DB, Temporal, Ollama), config examples table, root sections, merge policies. |
| [**configuration-reference.md**](configuration-reference.md) | **Full schema:** Every config field (feature flags, worker, temporal, activity, stageOrder, pipelines, plugins, dynamicPlugins, queue topology), validation, UI/drag-and-drop guidance. |
| [**architecture.md**](architecture.md) | High-level architecture, package layout, bootstrap flow, config loading (Redis → DB → file, `config/<CONFIG_KEY>.json`). |
| [**developerDoc.md**](developerDoc.md) | Developer guide: workflows, Temporal UI, infrastructure, config loading and default task queue. |
| [**features.md**](features.md) | Feature list, use cases, feature flags, Temporal qualities, plugin types, stage result envelope, human signal, streaming, determinism. |
| [**plugin-contract.md**](plugin-contract.md) | Authoritative plugin contract: StageHandler, config (e.g. `config/<CONFIG_KEY>.json`), registration, pipeline nodes, dynamic JARs. |

---

## How-to

| Document | Description |
|----------|-------------|
| [**temporal-ui-rag-flows.md**](temporal-ui-rag-flows.md) | Step-by-step: run document-ingestion (train) and question-answer flows from Temporal Web UI. |

---

## Config and env (summary)

- **Load order:** Redis → DB → file. When found in file, config is written back to Redis and DB so other pods can use it.
- **File path:** **`config/<CONFIG_KEY>.json`** when `CONFIG_FILE_PATH` is unset (e.g. `config/default.json`). Multiple files allowed (e.g. `config/production.json`); set **`CONFIG_KEY`** to choose.
- **Redis key:** `olo:engine:config:<CONFIG_KEY>:<version>` (e.g. `olo:engine:config:default:1.0`).
- **Example configs:** See [config-reference.md — Config examples](config-reference.md#config-examples-dedicated-per-use-case) and the **`config-examples/`** directory (minimal, full, stages, multi-pipeline, document-ingestion, chat-only, RAG, production).
