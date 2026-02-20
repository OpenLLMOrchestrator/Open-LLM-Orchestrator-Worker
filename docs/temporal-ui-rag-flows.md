<!-- Copyright 2026 Open LLM Orchestrator contributors. Licensed under the Apache License, Version 2.0; see LICENSE file. -->

# Temporal UI: RAG flows (train + question/answer)

Use these steps in **Temporal Web UI** (Temporal UI) to run the **document-ingestion** (train) and **question-answer** flows.

**Prerequisites**

- Temporal server running (e.g. `localhost:7233`).
- Worker running and connected to the same server and namespace, polling the task queue from config (default `core-task-queue`).
- Namespace: **default** (unless you changed it in config).

---

## 1. Train flow (single document)

Use this to ingest one document so it can be used for RAG. Run once per document (or batch).

### 1.1 Open “Start Workflow”

- In Temporal UI, go to **Workflows** (or **Namespaces** → **default** → **Workflows**).
- Click **Start Workflow**.

### 1.2 Set workflow parameters

| Field | Value |
|-------|--------|
| **Workflow Type** | `CoreWorkflowImpl` |
| **Task Queue** | `core-task-queue` |
| **Workflow ID** | e.g. `rag-train-doc-001` or `rag-train-{timestamp}` |
| **Namespace** | `default` (if prompted) |

### 1.3 Set workflow input (JSON)

Use a single argument: the **ExecutionCommand** as JSON.

**Option A – Ingest from a sample file**

Read the content of `samples/rag-docs/product-faq.txt` or `samples/rag-docs/company-policy.txt` and put it in the `document` field:

```json
{
  "pipelineName": "document-ingestion",
  "input": {
    "document": "Open LLM Orchestrator Worker – Product FAQ\n\nWhat is the Open LLM Orchestrator Worker?\nThe worker runs config-driven pipelines..."
  }
}
```

**Option B – Minimal (short doc)**

```json
{
  "pipelineName": "document-ingestion",
  "input": {
    "document": "This is a sample document for RAG. It will be tokenized and stored for retrieval."
  }
}
```

Optional fields you can add on the same level as `pipelineName` and `input`: `operation`, `tenantId`, `userId`, `metadata`.

### 1.4 Start the workflow

- Click **Start** (or **Run**).
- Open the run and check **History** / **Events** to confirm the workflow and activities complete.

---

## 2. Train flow (ingest folder into vector DB)

Use this to read **all files** from a folder and put them into the vector DB. The plugin reads every file (e.g. under `samples/rag-docs`), outputs chunks, and the vector store capability stores them.

### 3.1 Open "Start Workflow"

- **Workflows** → **Start Workflow**.

### 2.2 Set workflow parameters

| Field | Value |
|-------|--------|
| **Workflow Type** | `CoreWorkflowImpl` |
| **Task Queue** | `core-task-queue` |
| **Workflow ID** | e.g. `rag-train-folder-001` |
| **Namespace** | `default` (if prompted) |

### 2.3 Set workflow input (JSON)

**Required:** `input.folderPath` — path to the folder. When running in a container, this is **relative to the RAG shared folder** (see below). Otherwise relative to the worker’s working directory, or absolute.

**Optional:** `input.fileExtensions` — comma-separated (e.g. `.txt,.md`). Default: `.txt,.md`.  
**Optional:** `input.recursive` — `true` to include subfolders. Default: `false`.

**Shared folder (container):** The worker exposes a **shared folder** for use by any plugin (e.g. upload, document-ingestion, file search). In Docker it is created at `/app/shared` and declared as a `VOLUME`; mount a host directory there. Config: `sharedFolderPath` (or env `SHARED_FOLDER_PATH`). The execution context key `sharedFolderPath` is set so plugins can resolve paths relative to this root.

**Example – ingest `samples/rag-docs` (relative path):**

```json
{
  "pipelineName": "document-ingestion-folder",
  "input": {
    "folderPath": "samples/rag-docs"
  }
}
```

**Example – with custom extensions and recursive:**

```json
{
  "pipelineName": "document-ingestion-folder",
  "input": {
    "folderPath": "samples/rag-docs",
    "fileExtensions": ".txt,.md,.json",
    "recursive": true
  }
}
```

### 2.4 Start the workflow

- Click **Start** (or **Run**). The workflow will read all matching files from the folder and pass them to the vector store capability.

---

## 3. Question/answer flow

Use this after you have ingested documents (or with stub retrieval) to get an answer from the pipeline (retrieval + LLM).

### 2.1 Open “Start Workflow”

- Same as above: **Workflows** → **Start Workflow**.

### 2.2 Set workflow parameters

| Field | Value |
|-------|--------|
| **Workflow Type** | `CoreWorkflowImpl` |
| **Task Queue** | `core-task-queue` |
| **Workflow ID** | e.g. `rag-qa-001` or `rag-qa-{timestamp}` |
| **Namespace** | `default` (if prompted) |

### 3.3 Set workflow input (JSON)

Single argument: **ExecutionCommand** with `pipelineName` and `input.question`:

```json
{
  "pipelineName": "question-answer",
  "input": {
    "question": "How do I ingest documents for RAG?"
  }
}
```

Another example:

```json
{
  "pipelineName": "question-answer",
  "input": {
    "question": "What task queue does the worker use?"
  }
}
```

### 3.4 Start the workflow

- Click **Start** (or **Run**).
- In the workflow result/history, the last capability output will contain the **response** from the LLM (stub or real, depending on your plugin).

---

## Quick reference

| Flow | Workflow Type | Task Queue | pipelineName | input |
|------|----------------|------------|--------------|--------|
| **Train (single doc)** | `CoreWorkflowImpl` | `core-task-queue` | `document-ingestion` | `{ "document": "<full text>" }` |
| **Train (folder → vector DB)** | `CoreWorkflowImpl` | `core-task-queue` | `document-ingestion-folder` | `{ "folderPath": "<path>", optional "fileExtensions", "recursive" }` |
| **Question/answer** | `CoreWorkflowImpl` | `core-task-queue` | `question-answer` | `{ "question": "<your question>" }` |

If your config uses a different **task queue** or **namespace**, use those values in Temporal UI instead of the defaults above.

---

## How to debug

When the **DEBUGGER** feature is enabled, you can stream execution state to Redis for inspection (e.g. by a debug UI). Each node (capability, group, plugin, condition, expression) gets a unique UUID; state is written to Redis on **pre** and **post** for every node.

### 1. Enable DEBUGGER in config

Add **`DEBUGGER`** to **`enabledFeatures`** in your engine config (e.g. `config/default.json` or `config/debug.json`):

```json
"enabledFeatures": [
  "VERSIONED_STATE",
  "STAGE_RESULT_ENVELOPE",
  "DEBUGGER",
  ...
]
```

### 2. Set debug and debugID in workflow input

In the **ExecutionCommand** (workflow input JSON), set **`debug`** to `true` and **`debugID`** to a unique value (e.g. a UUID). Optionally set **`operation`** to identify the run (e.g. pipeline or use-case name).

**Example – question-answer with debug:**

```json
{
  "pipelineName": "question-answer",
  "operation": "question-answer",
  "debug": true,
  "debugID": "550e8400-e29b-41d4-a716-446655440000",
  "input": {
    "question": "How do I ingest documents for RAG?"
  }
}
```

**Example – document-ingestion-folder with debug:**

```json
{
  "pipelineName": "document-ingestion-folder",
  "operation": "document-ingestion-folder",
  "debug": true,
  "debugID": "550e8400-e29b-41d4-a716-446655440000",
  "input": {
    "folderPath": "samples/rag-docs"
  }
}
```

### 3. Where debug data is written

- **Redis keys** (per run):  
  - `olo:debug:<debugID>:ExecutionTree` – execution tree (plan + executionTreeRoots + capabilityNodeIds) for the current node.  
  - `olo:debug:<debugID>:context` – command, versionedState, and related context.
- State is **updated on every node** (before and after each capability, group, plugin, condition, expression). Each payload includes **`recordId`**, **`debugId`**, and **`executionNodeId`** (UUID per node).

### 4. Using the debug session

Use **`debugID`** as the session id: your debug UI or tool can read from Redis with that key prefix to show the execution tree and context as the run progresses. The same **`debugID`** can be passed when starting the workflow so you can correlate Temporal workflow ID with your debug session.

---

## Sample docs for RAG input

Two sample documents are in **`samples/rag-docs/`**:

- **`product-faq.txt`** – Product FAQ (worker, pipelines, RAG, config).
- **`company-policy.txt`** – Company policy (AI, data, ingestion, question-answer).

To use them in the **train** flow, read the file and set `input.document` to that text in the Start Workflow input JSON.
