# Plugins (move to separate repos later)

Four plugin packages, each in its own folder for easy extraction:

| Package | Plugin | Role |
|--------|--------|------|
| `plugin.vectordb` | `VectorStoreRetrievalPlugin` | Store document chunks (doc pipeline) or retrieve chunks for a question (QA pipeline). |
| `plugin.llm` | `Llama32ModelPlugin` | Call Llama 3.2 with context + question, return response. |
| `plugin.tokenizer` | `DocumentTokenizerPlugin` | Split document content into chunks for embedding/storage. |
| `plugin.folder` | `FolderIngestionPlugin` | Read all files from a folder and output as chunks for the vector store. |

## Pipelines (in `config/engine-config.json`)

- **document-ingestion** — Input: `{ "document": "<text>" }`. Runs: tokenizer → vector store (store chunks).
- **document-ingestion-folder** — Input: `{ "folderPath": "<path>", optional "fileExtensions": ".txt,.md", optional "recursive": false }`. Runs: folder ingestion (read all files) → vector store (store chunks).
- **question-answer** — Input: `{ "question": "<question>" }`. Runs: vector store (retrieve) → LLM (response).

Workflow payload: set `pipelineName` to `"document-ingestion"`, `"document-ingestion-folder"`, or `"question-answer"` and pass the corresponding input.

## Registration

Handlers are registered by FQCN in `BuildActivityRegistryStep` so config can reference them by class name. When moving a plugin to another repo, add a similar registration step or a plugin SPI that this worker discovers.
