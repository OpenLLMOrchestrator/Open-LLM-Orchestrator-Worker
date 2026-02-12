# Open LLM Orchestrator Worker

Temporal-native RAG execution worker.

This module:
- Registers RagWorkflow
- Executes embedding, retrieval and model activities
- Polls rag-task-queue

## Run

Ensure Temporal is running locally on localhost:7233

Then:

./gradlew run
