<!-- Copyright 2026 Open LLM Orchestrator contributors. Licensed under the Apache License, Version 2.0; see LICENSE file. -->

# Sample RAG documents

Use these as **document** input for the **document-ingestion** (train) pipeline.

| File | Description |
|------|-------------|
| `product-faq.txt` | Product FAQ: worker, pipelines, document ingestion, question-answer, config. |
| `company-policy.txt` | Company policy: AI usage, document ingestion, question-answer, operations. |

**How to use in Temporal UI**

1. Start a workflow with **Workflow Type** `CoreWorkflowImpl`, **Task Queue** `core-task-queue`.
2. Set **Workflow input** to JSON with `pipelineName: "document-ingestion"` and `input.document` set to the full text of one of these files (or both concatenated).
3. See [Temporal UI: RAG flows](../../docs/temporal-ui-rag-flows.md) for full steps for train and question/answer.
