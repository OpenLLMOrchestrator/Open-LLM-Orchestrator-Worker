# Plugin JARs and plugins module

This directory serves two purposes:

1. **Drop folder for JARs** — Place plugin JAR files here (see below). They are bundled at build time.
2. **Gradle `plugins` module** — Contains sample and stub plugins (contract-only). Build with `./gradlew :plugins:jar`; the JAR is in `plugins/build/libs/sample-plugins-0.0.1.jar`. Copy it into this folder to bundle it.

---

## Plugins in this module (use cases)

| Plugin | Type | Use case | Description |
|--------|------|----------|-------------|
| **SampleEchoPlugin** | Tool | Dynamic / tools | Echoes input to output; available as tool for planner. |
| **StubFilterPlugin** | Filter | Static / document ingestion | Tokenizes `document` into one chunk (stub). |
| **StubRetrievalPlugin** | VectorStore | Static / RAG | Stores chunks or returns stub chunk from `question`. |
| **StubModelPlugin** | Model | Static / chat or RAG | Stub response from `question` (no LLM). |
| **StubRefinementPlugin** | Refinement | Static / post-process | Formats result as `ANS: "..."`. |

All implement ContractCompatibility, PlannerInputDescriptor, and PluginTypeDescriptor. Use in **static** pipelines (reference by FQCN in config and `plugins` allow-list), or load via **dynamicPluginJars** for dynamic/planner use cases.

---

## Drop folder: plugin JARs

Place **plugin JAR files** here. They are:

- **Ignored by Git** (see `.gitignore`) so you can add local or proprietary plugins without committing them.
- **Bundled at build time**: when you run `./gradlew installDist` (or `distZip` / `distTar`), all `*.jar` files in this folder are copied into the distribution under `plugins/`.

After building, the distribution layout includes:

```text
worker/
  bin/
  lib/
  plugins/    <-- your plugin JARs (if any were in this folder at build time)
```

**Option 1 — One JAR, multiple plugins:** set `dynamicPluginJars` to a list of JAR paths; the worker loads all StageHandler implementations from each JAR and registers each by its `name()`:

```json
"dynamicPluginJars": ["plugins/sample-plugins-0.0.1.jar"]
```

**Option 2 — One plugin per JAR:** set `dynamicPlugins` to a map of plugin name → JAR path; the worker loads the first StageHandler from each JAR:

```json
"dynamicPlugins": {
  "my-plugin": "plugins/my-plugin-1.0.0.jar"
}
```

Add plugin FQCNs to `plugins` if you use an allow-list. See [Configuration Reference](../docs/configuration-reference.md) and [Plugin Contract](../docs/plugin-contract.md).
