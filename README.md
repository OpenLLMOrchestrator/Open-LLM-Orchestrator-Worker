<!-- Copyright 2026 Open LLM Orchestrator contributors. Licensed under the Apache License, Version 2.0; see LICENSE file. -->

# Open LLM Orchestrator Worker

A **Temporal-native worker** that runs **config-driven pipelines** for document ingestion, retrieval, and LLM-based question answering (RAG). All behavior is defined in configuration—pipelines, capabilities, plugins, timeouts, and merge policies—with no hardcoded flows in the engine.

---

## What it does

- **Runs workflows** on a Temporal task queue (`CoreWorkflow`): you send an `ExecutionCommand` with a pipeline name and input; the worker executes the pipeline and returns the accumulated output (including the LLM **result**).
- **Pipelines** are defined in config (e.g. `config/default.json` or `config/<CONFIG_KEY>.json`): recursive GROUP/STAGE trees or capability-based flows with SYNC/ASYNC groups. Each capability is implemented by a **plugin** (activity) registered by name.
- **Built-in pipelines** in the default config:
  - **document-ingestion** — Tokenize a document and store chunks in the vector store.
  - **document-ingestion-folder** — Read all files from a folder and store them as chunks.
  - **question-answer** — Retrieve chunks for a question and get an LLM response (e.g. via local Ollama).

Plugins (tokenizer, vector store, folder ingestion, LLM) are included in this repo and can be replaced or extended; the engine stays config-driven and plugin-agnostic.

---

## Features

- **Config-first**: Pipelines, timeouts, retries, merge policies, and task queue come from config (file, Redis, or DB). Env vars override connection settings in production.
- **Temporal-native**: Single workflow type (`CoreWorkflowImpl`); activities for each capability; workflow returns the full result map (including `result` from the LLM plugin).
- **Extensible**: Add pipelines and plugins by config and registration; async merge is a **plugin** invoked as an activity before exiting each ASYNC group.
- **RAG-ready**: Sample pipelines and plugins for document chunking, vector store (stub/real), and LLM (Ollama; URL and model via `OLLAMA_BASE_URL`, `OLLAMA_MODEL`).

---

## Prerequisites

| Component    | Purpose |
|-------------|---------|
| **Java 21+** | Build and run the worker. |
| **Temporal** | Workflow orchestration (default: `localhost:7233`). |
| **Ollama** (optional) | For real LLM responses; set `OLLAMA_BASE_URL` and `OLLAMA_MODEL` (see [Config reference](docs/config-reference.md)). |
| **Redis** (optional) | Config storage / cache when not using file-only config. |
| **PostgreSQL** (optional) | Config storage when not using file-only config. |

For local development, **Temporal** is required; **Ollama** is optional (the LLM plugin can run with a stub if Ollama is not available).

---

## Quick start

1. **Start Temporal** (e.g. Docker or local install) on `localhost:7233`.

2. **(Optional)** Start **Ollama** and pull a model, e.g.:
   ```bash
   ollama run llama3.2:latest
   ```

3. **Run the worker**:
   ```bash
   ./gradlew run
   ```
   The worker loads config in order **Redis → DB → file**. The file path is **`config/<CONFIG_KEY>.json`** (default: `config/default.json`) when `CONFIG_FILE_PATH` is unset. Set `CONFIG_KEY` in the environment to pick a different config (e.g. `config/production.json`). The worker then connects to Temporal and polls the task queue (`core-task-queue` by default).

4. **Trigger a workflow** (e.g. from [Temporal Web UI](https://docs.temporal.io/develop/ui)):
   - **Workflow Type:** `CoreWorkflowImpl`
   - **Task Queue:** `core-task-queue`
   - **Input** (single JSON argument — ExecutionCommand):
   ```json
   {
     "pipelineName": "question-answer",
     "input": { "question": "What is the Open LLM Orchestrator Worker?" }
   }
   ```
   The workflow **result** is a map containing all capability outputs; the LLM plugin sets **`result`** (and `response`) to the model’s answer.

---

## Pipelines (default config)

| Workflow | Pipeline ID | Input | Description |
|----------|-------------|--------|-------------|
| **CoreWorkflowImpl** | **document-ingestion** | `{ "document": "<text>" }` | Tokenize document → store chunks in vector store. |
| **CoreWorkflowImpl** | **document-ingestion-folder** | `{ "folderPath": "<path>", optional "fileExtensions", "recursive" }` | Read folder → tokenize → store chunks. |
| **CoreWorkflowImpl** | **question-answer** | `{ "question": "<question>" }` | Retrieve chunks → LLM (Ollama) → `result`, `response`, and formatted `output` (ANS: "..."). |
| **CoreWorkflowImpl** | **rag-*** (e.g. rag-mistral, rag-llama3.2) | `{ "question": "..." }` | RAG: retrieve + one model (mistral, llama3.2, phi3, gemma2-2b, qwen2-1.5b). |
| **CoreWorkflowImpl** | **chat-*** (e.g. chat-mistral, chat-llama3.2) | `{ "question": "..." }` or `{ "messages": [...] }` | Chat only (no RAG) with one model. |
| **CoreWorkflowImpl** | **query-all-models** | `{ "question": "..." }` or `{ "messages": [...] }` | Query all five models in parallel; merged output: `response from X: "..."` per model. |

**Full list** (workflow name, pipeline ID, payload, summary): see **[config/README.md](config/README.md)**.

Workflow **input** is always an **ExecutionCommand**: `{ "pipelineName": "<id>", "input": { ... } }`. The workflow **return value** is a **Map&lt;String, Object&gt;** (accumulated output), including `result` and often `output` from the LLM/post-process capabilities.

---

## Configuration

- **Location**: Config is loaded once at startup in order **Redis** → **DB** → **file**. The file path is **`config/<CONFIG_KEY>.json`** when `CONFIG_FILE_PATH` is unset (e.g. `config/default.json` for `CONFIG_KEY=default`). Multiple config files can live in `config/` (e.g. `default.json`, `production.json`); set **`CONFIG_KEY`** to choose which one to load. When config is loaded from file, it is written back to Redis and DB so other pods can use it.
- **Content**: `worker` (task queue), `temporal`, `activity` (timeouts/retry), `pipelines` (required), optional `redis`, `database`, `capabilityOrder`/`stageOrder`, `mergePolicies`. See [Config reference](docs/config-reference.md).
- **Production**: Queue name, Redis, and DB URLs are overridden by **environment variables** (e.g. `QUEUE_NAME`, `REDIS_HOST`, `DB_URL`, `CONFIG_KEY`). See the [config reference — environment variables](docs/config-reference.md#production-environment-variables-container) section.

Example configs for each use case (minimal, full, capabilities, multi-pipeline, document-ingestion, chat-only, RAG, production) are in **`config/`** (see [config reference](docs/config-reference.md)).

---

## Project structure and docs

| Path | Description |
|------|-------------|
| **`config/`** | Config files: **`config/<CONFIG_KEY>.json`** (e.g. `config/default.json`). Load order: Redis → DB → file; file choice by `CONFIG_KEY`. |
| **`config/`** | Example configs per use case (minimal, full, capabilities, multi-pipeline, document-ingestion, chat-only, RAG, production). |
| **`docs/README.md`** | Documentation index (all docs and config/env summary). |
| **`docs/architecture.md`** | High-level architecture and package layout. |
| **`docs/config-reference.md`** | Full config reference, env vars, and merge policies. |
| **`docs/temporal-ui-rag-flows.md`** | Step-by-step: running train and question-answer flows from Temporal UI. |
| **`docs/developerDoc.md`** | Developer guide and workflow details. |
| **`src/main/java/.../plugin/`** | Plugins: LLM (Ollama), vector store, tokenizer, folder ingestion. |

---

## Running workflows (summary)

- **Workflow Type:** `CoreWorkflowImpl`
- **Task Queue:** from config (default `core-task-queue`)
- **Input:** one JSON object, e.g. `{ "pipelineName": "question-answer", "input": { "question": "..." } }`
- **Output:** workflow returns a **map**; for question-answer, the key **`result`** holds the LLM answer.

Detailed steps and sample payloads: [Temporal UI: RAG flows](docs/temporal-ui-rag-flows.md).

---

## Docker

- **Env file:** Copy `.env.example` to `.env` and set values (Temporal, queue, Redis, DB, Ollama, `CONFIG_KEY`). All connection and plugin URLs are driven by env; see [Config reference — environment variables](docs/config-reference.md#production-environment-variables-container).
- **Run with Compose:** `docker compose up -d` builds the worker image and starts the worker, Temporal, Redis, Postgres, and Ollama. The worker uses `TEMPORAL_TARGET=temporal:7233`, `OLLAMA_BASE_URL=http://ollama:11434`, and mounts `./config` for engine config. Config file is **`config/<CONFIG_KEY>.json`** (e.g. `/app/config/default.json`). Override any variable in `.env`.
- **Standalone image:** `docker build -t open-llm-orchestrator-worker .` then run with env vars or `--env-file .env` and a volume for `config/` (ensure the chosen `config/<CONFIG_KEY>.json` exists).