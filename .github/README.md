# GitHub Actions workflows

## Workflows

| Workflow | Trigger | Purpose |
|----------|---------|---------|
| **publish-plugin-contract.yml** | Push to `main`/`master`, or release published | Publishes the `plugin-contract` module to GitHub Packages (Maven). Used by Open-LLM-Orchestrator-plugins and other consumers. No changes needed for worker plugin-zip or shadow JAR setup. |
| **docker-publish.yml** | Push to `main`/`master` | Builds the Docker image and pushes to Docker Hub. Uses `Dockerfile`, which builds the worker **shadow (fat) JAR** (`unpackPluginZips` + `shadowJar`) and runs `java -jar worker.jar`. Requires `plugins/` directory in the repo; add `plugins/*.zip` in the build context to include plugin JARs in the image. |

## Secrets

- **docker-publish**: `DOCKERHUB_USERNAME`, `DOCKERHUB_TOKEN` (repo secrets).
- **publish-plugin-contract**: Uses `GITHUB_TOKEN` for GitHub Packages; no extra secrets.

## Notes

- No webhooks or git hooks are configured in this repo; only these Actions run on push/release.
- To ship plugins inside the Docker image, ensure plugin zip(s) are present under `plugins/` when the image is built (e.g. copy from Open-LLM-Orchestrator-plugins build or a release artifact).
