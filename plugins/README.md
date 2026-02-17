# Plugins (compile-time, same worker JAR)

All plugins available in this folder are **compile-time** dependencies. Their code is built into the **same worker JAR** after build (no separate plugin JARs loaded at runtime for these).

---

## What counts as “plugins in the folder”

1. **In-repo `plugins` project** — If `plugins/build.gradle` exists, the worker adds `implementation project(':plugins')`. Plugin classes and their `META-INF/services` entries are included in the worker fat JAR.
2. **Plugin zips (from another repo)** — Place **`*.zip`** files here. The **unpackPluginZips** task unpacks each zip, expands **olo** (directory or `olo.zip`) if present, collects all JARs into `build/plugin-jars/`. Those JARs are added as `implementation` dependencies, so their contents are merged into the worker fat JAR (Shadow JAR with merged service files).
3. **Loose JARs** — Any **`*.jar`** files directly in this folder are also `implementation` dependencies and end up inside the worker JAR.

---

## Build

- Run `./gradlew unpackPluginZips` (or any task that needs plugin JARs; `compileJava` and `shadowJar` depend on it).
- The **fat JAR** is produced by:  
  `./gradlew shadowJar`  
  Output: `build/libs/open-llm-orchestrator-worker-0.0.1-all.jar`. This JAR contains the worker and all compile-time plugin code; run with:  
  `java -jar build/libs/open-llm-orchestrator-worker-0.0.1-all.jar`.
- `./gradlew installDist` still builds a distribution with `lib/` (worker + dependencies). For a single JAR deployment, use `shadowJar` and run the `-all` JAR as above.

At **runtime**, plugins are discovered from the **classpath** via `ServiceLoader.load(CapabilityHandler.class)` (no separate “plugins directory” loading for compile-time plugins). You can still add extra runtime JARs via config **dynamicPluginJars** (list of paths) and **dynamicPlugins** (plugin name → JAR path). See [Configuration Reference](../docs/configuration-reference.md) and [Plugin Contract](../docs/plugin-contract.md).

---

## Zip layout (for plugin zips from another repo)

Each zip in this folder is expanded **twice** when present:

1. **First expansion:** the main `*.zip` is unpacked.
2. **Second expansion:** every **`*.olo`** file found inside (at any path) is treated as a zip and unpacked; all `**/*.jar` from each expanded `.olo` are collected into `build/plugin-jars/`.

Because JARs from different `.olo` files often have the same name (e.g. `plugin.jar`), each copied JAR gets a **unique filename**: `<oloBasename>-<originalJarName>`. If that would collide, a number is inserted: `<oloBasename>-<n>-<originalJarName>` (e.g. `FolderIngestion-plugin.jar`, `VectorStore-plugin.jar`).

Supported layouts:

- **Multiple .olo files** (recommended): the zip contains one or more `*.olo` files (each is a zip). Each `.olo` is expanded and JARs inside it are collected.
- **Fallback:** a single **`olo/`** directory with JARs, or a single **`olo.zip`** that contains JARs, or JARs at the root of the main zip.

Plugin JARs must declare `META-INF/services/com.openllmorchestrator.worker.contract.CapabilityHandler` (one implementation class name per line) so they are discoverable when merged into the worker JAR.
