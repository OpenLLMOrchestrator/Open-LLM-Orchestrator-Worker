Open LLM Orchestrator – Worker Developer Guide

This document explains:

Available workflows

How they work internally

How to trigger them via Temporal UI

Infrastructure dependencies

Expected behavior

🏗 Architecture Overview

The Worker is responsible for:

Document ingestion

Vector embedding

Qdrant storage

Retrieval

LLM generation

Chat memory persistence

It runs as a Temporal Worker and polls:

rag-task-queue

⚙️ Required Infrastructure

Before running workflows, ensure:

Component	Port	Purpose
Temporal	7233	Workflow orchestration
Temporal UI	8080	Manual workflow execution
Qdrant	6333	Vector database
Redis	6379	Chat memory
Ollama	11434	LLM + Embeddings
🚀 Workflow 1: IngestWorkflow
Purpose

Reads a document file, chunks it, embeds chunks, and stores vectors in Qdrant.

Each document gets its own collection:

rag_<documentId>

Input
{
"documentId": "temporal-doc",
"filePath": "D:\\open-llm-orchestrator-docker-example\\docs\\temporal.txt"
}

Internal Steps

Resolve collection name:

rag_<documentId>


Ensure collection exists (auto-create if missing)

Read file

Chunk content

Embed each chunk via Ollama

Upsert into Qdrant with payload:

{
"text": "...chunk...",
"documentId": "temporal-doc"
}

Expected Output
{
"documentId": "temporal-doc",
"chunksCreated": 5
}

How to Trigger via Temporal UI

Open:

http://localhost:8080


Click Start Workflow

Workflow Type:

IngestWorkflow


Task Queue:

rag-task-queue


Provide JSON input.

💬 Workflow 2: ChatWorkflow
Purpose

Performs full RAG query with chat memory.

Steps:

Embed question

Retrieve topK chunks from Qdrant

Load chat history from Redis

Generate answer using Ollama

Save chat history

Input
{
"documentId": "temporal-doc",
"chatId": "chat-1",
"question": "What is Temporal?",
"topK": 3
}

Retrieval Logic

Searches in:

rag_<documentId>


Using cosine similarity.

Returns topK matching chunks.

Response
{
"answer": "...",
"modelUsed": "llama3",
"sources": [
"chunk1 text...",
"chunk2 text..."
],
"latencyMs": 12345,
"promptTokens": 40,
"completionTokens": 55
}

Redis Chat Memory

Stored under key:

chat:context:<chatId>


Example:

chat:context:chat-1


Stored entries:

User: What is Temporal?
Assistant: Temporal is ...

Triggering via Temporal UI

Start workflow

Select:

ChatWorkflow


Use same task queue:

rag-task-queue


Provide JSON input.

🧠 Workflow 3: RagWorkflow (Legacy)

This is a simpler RAG workflow without chat memory.

Input:

{
"documentId": "temporal-doc",
"question": "What is Temporal?"
}


Uses:

Embed

Retrieve

Generate

No Redis memory.

🔎 Debugging Guide
Verify Collection Exists
curl http://localhost:6333/collections

Inspect Stored Vectors
curl http://localhost:6333/collections/rag_temporal-doc

Check Redis Memory
docker exec -it redis redis-cli
LRANGE chat:context:chat-1 0 -1

Common Issues
Problem	Cause
404 on upsert	Collection missing
404 on search	Wrong collection name
Empty context	Payload missing
NOT_FOUND activity error	Activity timeout too low
Slow generation	Ollama CPU inference
🧩 Dynamic Embedding Dimension

Collection dimension is auto-detected from:

embeddingVector.size()


No hardcoding required.

🔥 Production Notes

Each document has isolated vector collection

Collection auto-created if missing

Embedding dimension dynamic

RAG fully grounded

Chat memory persistent

Temporal guarantees execution reliability

🧭 Future Enhancements

Planned upgrades:

Metadata filtering

Multi-tenant collection naming

Streaming LLM responses

gRPC-based model container

Control-plane service

Model routing & scaling

🏁 End-to-End Flow
IngestWorkflow
↓
Qdrant (vector store)
↓
ChatWorkflow
↓
Redis (memory)
↓
LLM Answer

🔥 You Now Have

A Temporal-native AI orchestration worker capable of:

Document ingestion

Semantic search

Context-aware generation

Persistent chat sessions

Fully isolated vector storage