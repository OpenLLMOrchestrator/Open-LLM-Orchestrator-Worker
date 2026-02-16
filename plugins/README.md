# Plugin JARs and plugins module

This directory serves two purposes:

1. **Drop folder for JARs** — Place plugin JAR files here (see below). They are bundled at build time.
2. **Gradle `plugins` module** — Contains sample plugins (e.g. `SampleEchoPlugin`) that depend only on the **plugin-contract** module. Build with `./gradlew :plugins:jar`; the JAR is in `plugins/build/libs/`. You can copy it here to bundle it.

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

Reference them in `config/engine-config.json` under `dynamicPlugins` using paths relative to the install directory, for example:

```json
"dynamicPlugins": {
  "my-plugin": "plugins/my-plugin-1.0.0.jar"
}
```

Or use absolute paths. See [Configuration Reference](../docs/configuration-reference.md) and [Plugin Contract](../docs/plugin-contract.md).
