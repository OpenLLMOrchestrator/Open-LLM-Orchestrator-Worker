# Engine config

## Workflow

- **Workflow Type:** `CoreWorkflowImpl`
- **Task Queue:** from config (default `core-task-queue`)
- **Input:** Single JSON argument — an **ExecutionCommand** with `pipelineName` and `input`. The workflow returns a **Map** (accumulated output); model pipelines also set `output` (e.g. `ANS: "..."` or merged multi-model text).

---

## Pipelines reference

All pipelines are defined in `engine-config.json`. Use `pipelineName` in the ExecutionCommand to select one.

| Pipeline ID | Input payload | Summary |
|-------------|----------------|---------|
| **document-ingestion** | `{ "document": "<text>" }` | Tokenize document and store chunks in the vector store (FILTER → RETRIEVAL). |
| **document-ingestion-folder** | `{ "folderPath": "<path>", "fileExtensions": ".txt,.md", "recursive": false }` | Read all files from a folder, tokenize, and store chunks (FILTER → RETRIEVAL). |
| **question-answer** | `{ "question": "<question>" }` | Retrieve chunks for the question, then run LLM (default model). Output in `result`, `response`, and formatted `output` (ANS: "..."). |
| **llama-oss** | `{ "question": "..." }` or `{ "messages": [...] }` | Chat only (no RAG). Single model (Llama). |
| **openai-oss** | `{ "question": "..." }` or `{ "messages": [...] }` | Chat only (no RAG). Single model (OpenAI placeholder / Ollama). |
| **both** | `{ "question": "..." }` or `{ "messages": [...] }` | Chat only (no RAG). Both models placeholder. |
| **rag-llama-oss** | `{ "question": "<question>" }` | RAG: retrieve chunks then LLM (Llama). Output formatted as ANS: "...". |
| **rag-openai-oss** | `{ "question": "<question>" }` | RAG: retrieve chunks then LLM (OpenAI placeholder). |
| **rag-both** | `{ "question": "<question>" }` | RAG: retrieve chunks then LLM (both placeholder). |
| **rag-mistral** | `{ "question": "<question>" }` | RAG: retrieve chunks then **mistral:latest**. Output ANS: "...". |
| **rag-llama3.2** | `{ "question": "<question>" }` | RAG: retrieve chunks then **llama3.2:latest**. Output ANS: "...". |
| **rag-phi3** | `{ "question": "<question>" }` | RAG: retrieve chunks then **phi3:latest**. Output ANS: "...". |
| **rag-gemma2-2b** | `{ "question": "<question>" }` | RAG: retrieve chunks then **gemma2:2b**. Output ANS: "...". |
| **rag-qwen2-1.5b** | `{ "question": "<question>" }` | RAG: retrieve chunks then **qwen2:1.5b**. Output ANS: "...". |
| **chat-mistral** | `{ "question": "..." }` or `{ "messages": [...] }` | Chat only with **mistral:latest**. Output ANS: "...". |
| **chat-llama3.2** | `{ "question": "..." }` or `{ "messages": [...] }` | Chat only with **llama3.2:latest**. Output ANS: "...". |
| **chat-phi3** | `{ "question": "..." }` or `{ "messages": [...] }` | Chat only with **phi3:latest**. Output ANS: "...". |
| **chat-gemma2-2b** | `{ "question": "..." }` or `{ "messages": [...] }` | Chat only with **gemma2:2b**. Output ANS: "...". |
| **chat-qwen2-1.5b** | `{ "question": "..." }` or `{ "messages": [...] }` | Chat only with **qwen2:1.5b**. Output ANS: "...". |
| **query-all-models** | `{ "question": "..." }` or `{ "messages": [...] }` | Query **all five models** (mistral, llama3.2, phi3, gemma2:2b, qwen2:1.5b) in **async parallel**; wait for all to finish; merge output as `response from mistral: "..."` \n `response from llama3.2: "..."` \n … in `output` and `result`. |

---

## Example ExecutionCommand payloads

**RAG (single model):**
```json
{
  "pipelineName": "rag-mistral",
  "input": { "question": "What is RAG?" }
}
```

**Chat (single model):**
```json
{
  "pipelineName": "chat-llama3.2",
  "input": { "question": "Explain async in one sentence." }
}
```

**Query all models (async, merged):**
```json
{
  "pipelineName": "query-all-models",
  "input": { "question": "What is 2 + 2?" }
}
```

**Document ingestion:**
```json
{
  "pipelineName": "document-ingestion",
  "input": { "document": "Your long text here..." }
}
```

Optional: you can pass `"modelId": "<model>"` in `input` to override the pipeline’s default model for chat/RAG pipelines where the plugin supports it.
