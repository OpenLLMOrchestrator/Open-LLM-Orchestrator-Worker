# Plugin Contract

This module defines the **plugin API** for the Open LLM Orchestrator Worker. Both the **worker** (this repo) and **plugin implementations** (this repo’s `plugins` module or an external plugin repo) depend only on this contract.

## Contents

- **`PluginContext`** — Plugin-facing execution context (read input, write output, optional suspend/break/agent/determinism).
- **`StageHandler`** — Core contract: `name()`, `execute(PluginContext)` → `StageResult`.
- **`StageResult`**, **`StageMetadata`**, **`DependencyRef`** — Result envelope and metadata.
- **`AgentContext`**, **`AgentMemoryStore`**, **`DeterminismPolicy`** — Optional agent and determinism types used by `PluginContext`.
- **`StreamObserver`**, **`StreamingStageHandler`**, **`CheckpointableStage`** — Optional streaming and checkpoint extensions.
- **`OutputContract`**, **`OutputContractValidator`**, **`OutputContractViolationException`** — Optional output schema validation.

## Usage

### Worker (this repo)

```groovy
dependencies {
    implementation project(':plugin-contract')
}
```

### External plugin repo

When the contract is published to Maven (see below), add:

```groovy
repositories {
    mavenCentral()
    maven { url = uri("https://maven.pkg.github.com/YOUR_ORG/open-llm-orchestrator-worker") }
}
dependencies {
    implementation 'com.openllm:plugin-contract:0.0.1'
}
```

Plugin JARs that are loaded dynamically must implement `com.openllmorchestrator.worker.contract.StageHandler` and declare it in `META-INF/services/com.openllmorchestrator.worker.contract.StageHandler`.

### Publishing to Maven (Git trigger)

A **GitHub Action** (`.github/workflows/publish-plugin-contract.yml`) publishes this module to **GitHub Packages** when:

- You **push to `main` or `master`** — publishes the current project version (e.g. `0.0.1`).
- You **publish a GitHub Release** — publishes the tag as the version (e.g. tag `v1.0.0` → artifact version `1.0.0`).

No extra secrets are required; the workflow uses `GITHUB_TOKEN` with `packages: write`. The artifact is published to `https://maven.pkg.github.com/OWNER/REPO` (your repository). To consume it from another repo, add that URL as a Maven repository and use the dependency above (and configure GitHub Packages auth if the repo is private).

See [Plugin Contract](../docs/plugin-contract.md) for the full contract document.
