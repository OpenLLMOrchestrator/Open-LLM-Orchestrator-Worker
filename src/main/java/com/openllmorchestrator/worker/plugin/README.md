# Plugins (move to separate repos later)

Four plugin packages, each in its own folder for easy extraction:

| Package | Plugin | Role |
|--------|--------|------|
| `plugin.vectordb` | `VectorStoreRetrievalPlugin` | Store document chunks (doc pipeline) or retrieve chunks for a question (QA/RAG pipeline). |
| `plugin.llm` | `Llama32ModelPlugin` | **RAG model plugin:** call Ollama with context + question, return response. Supports all models (mistral, llama3.2, phi3, gemma2:2b, qwen2:1.5b) via pipeline name or `input.modelId`. |
| `plugin.llm` | `Llama32ChatPlugin` | **Chat model plugin:** call Ollama with question/messages only (no retrieval). Supports all models via pipeline name or `input.modelId`. |
| `plugin.tokenizer` | `DocumentTokenizerPlugin` | Split document content into chunks for embedding/storage. |
| `plugin.folder` | `FolderIngestionPlugin` | Read all files from a folder and output as chunks for the vector store. |

## RAG plugins

RAG is implemented by **two plugins** used together in a pipeline:

1. **VectorStoreRetrievalPlugin** (`plugin.vectordb`) — Retrieval stage. For a question, retrieves chunks from the vector store and puts `retrievedChunks` into the context.
2. **Llama32ModelPlugin** (`plugin.llm`) — Model stage. Reads `retrievedChunks` and the question, builds a context prompt, and calls Ollama. Model is resolved from `input.modelId`, pipeline name (e.g. `rag-mistral`), or `OLLAMA_MODEL`.

All RAG pipelines (e.g. `rag-mistral`, `rag-llama3.2`, `rag-phi3`, `rag-gemma2-2b`, `rag-qwen2-1.5b`) use this same pair; the model is determined by the pipeline name or input.

## Chat plugins

Chat (no retrieval) uses a single plugin:

- **Llama32ChatPlugin** (`plugin.llm`) — Model stage only. Sends question or messages to Ollama. Pipelines: `chat-mistral`, `chat-llama3.2`, `chat-phi3`, `chat-gemma2-2b`, `chat-qwen2-1.5b`.

## Pipelines (in `config/engine-config.json`)

- **document-ingestion** — Input: `{ "document": "<text>" }`. Runs: tokenizer → vector store (store chunks).
- **document-ingestion-folder** — Input: `{ "folderPath": "<path>", optional "fileExtensions": ".txt,.md", optional "recursive": false }`. Runs: folder ingestion (read all files) → vector store (store chunks).
- **question-answer** — Input: `{ "question": "<question>" }`. Runs: vector store (retrieve) → LLM (response).

**RAG pipelines (retrieval + model, one per model):** `rag-mistral`, `rag-llama3.2`, `rag-phi3`, `rag-gemma2-2b`, `rag-qwen2-1.5b`. Input: `{ "question": "..." }` (optional `modelId` to override).

**Chat pipelines (model only):** `chat-mistral`, `chat-llama3.2`, `chat-phi3`, `chat-gemma2-2b`, `chat-qwen2-1.5b`. Input: `{ "question": "..." }` or `{ "messages": [...] }`.

Workflow payload: set `pipelineName` to the desired pipeline and pass the corresponding input.

## Registration

Handlers are registered by FQCN in `BuildActivityRegistryStep` so config can reference them by class name. When moving a plugin to another repo, add a similar registration step or a plugin SPI that this worker discovers.
