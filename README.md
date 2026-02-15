<!-- Copyright 2026 Open LLM Orchestrator contributors. Licensed under the Apache License, Version 2.0; see LICENSE file. -->

# Open LLM Orchestrator Worker

A **Temporal-native worker** that runs **config-driven pipelines** for document ingestion, retrieval, and LLM-based question answering (RAG). All behavior is defined in configuration—pipelines, stages, plugins, timeouts, and merge policies—with no hardcoded flows in the engine.

---

## What it does

- **Runs workflows** on a Temporal task queue (`CoreWorkflow`): you send an `ExecutionCommand` with a pipeline name and input; the worker executes the pipeline and returns the accumulated output (including the LLM **result**).
- **Pipelines** are defined in config (e.g. `config/engine-config.json`): recursive GROUP/STAGE trees or stage-based flows with SYNC/ASYNC groups. Each stage is implemented by a **plugin** (activity) registered by name.
- **Built-in pipelines** in the default config:
  - **document-ingestion** — Tokenize a document and store chunks in the vector store.
  - **document-ingestion-folder** — Read all files from a folder and store them as chunks.
  - **question-answer** — Retrieve chunks for a question and get an LLM response (e.g. via local Ollama).

Plugins (tokenizer, vector store, folder ingestion, LLM) are included in this repo and can be replaced or extended; the engine stays config-driven and plugin-agnostic.

---

## Features

- **Config-first**: Pipelines, timeouts, retries, merge policies, and task queue come from config (file, Redis, or DB). Env vars override connection settings in production.
- **Temporal-native**: Single workflow type (`CoreWorkflowImpl`); activities for each stage; workflow returns the full result map (including `result` from the LLM plugin).
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
   The worker loads config from `config/engine-config.json` (or Redis/DB if configured), connects to Temporal, and polls the task queue (`core-task-queue` by default).

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
   The workflow **result** is a map containing all stage outputs; the LLM plugin sets **`result`** (and `response`) to the model’s answer.

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

Workflow **input** is always an **ExecutionCommand**: `{ "pipelineName": "<id>", "input": { ... } }`. The workflow **return value** is a **Map&lt;String, Object&gt;** (accumulated output), including `result` and often `output` from the LLM/post-process stages.

---

## Configuration

- **Location**: Config is loaded once at startup from (in order) **Redis** → **DB** → **file**. Default file: `config/engine-config.json`.
- **Content**: `worker` (task queue), `temporal`, `activity` (timeouts/retry), `pipelines` (required), optional `redis`, `database`, `stageOrder`, `mergePolicies`. See [Config reference](docs/config-reference.md).
- **Production**: Queue name, Redis, and DB URLs are overridden by **environment variables** (e.g. `QUEUE_NAME`, `REDIS_HOST`, `DB_URL`). See the [config reference — environment variables](docs/config-reference.md#production-environment-variables-container) section.

Example config snippets and multiple pipeline examples are in **`docs/config-examples/`**.

---

## Project structure and docs

| Path | Description |
|------|-------------|
| **`config/engine-config.json`** | Default pipeline and worker config. |
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

- **Env file:** Copy `.env.example` to `.env` and set values (Temporal, queue, Redis, DB, Ollama, config path). All connection and plugin URLs are driven by env; see [Config reference — environment variables](docs/config-reference.md#production-environment-variables-container).
- **Run with Compose:** `docker compose up -d` builds the worker image and starts the worker, Temporal, Redis, Postgres, and Ollama. The worker uses `TEMPORAL_TARGET=temporal:7233`, `OLLAMA_BASE_URL=http://ollama:11434`, and mounts `./config` for engine config. Override any variable in `.env`.
- **Standalone image:** `docker build -t open-llm-orchestrator-worker .` then run with env vars or `--env-file .env` and a volume for `config/`.

### CI/CD — Push to Docker Hub on master

A GitHub Actions workflow (`.github/workflows/docker-publish.yml`) builds and pushes the Docker image to Docker Hub on every push to the **master** branch.

**Setup:** In your GitHub repo go to **Settings → Secrets and variables → Actions** and add:

| Secret | Description |
|--------|-------------|
| `DOCKERHUB_USERNAME` | Your Docker Hub username |
| `DOCKERHUB_TOKEN` | Docker Hub access token ([Create one](https://hub.docker.com/settings/security) under Account Settings → Security) |

The image will be published as `DOCKERHUB_USERNAME/open-llm-orchestrator-worker:latest` and also tagged with the short Git SHA (e.g. `...worker:abc1234`).

---

## License and contributing

See the repository license file. Contributions and issues are welcome; for deep dives, start with [Architecture](docs/architecture.md) and [Config reference](docs/config-reference.md).
