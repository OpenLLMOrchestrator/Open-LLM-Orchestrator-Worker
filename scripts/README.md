# Scripts

## check-ai-services (verify AI containers and endpoints)

Before running AI pipelines (e.g. **query-all-models**, **chat-***, **question-answer**), ensure the Ollama container is up and responding.

**PowerShell (Windows):**
```powershell
.\scripts\check-ai-services.ps1
```

**Bash (Linux / Mac / Git Bash):**
```bash
./scripts/check-ai-services.sh
```

**What it checks:**
- **Docker Compose** – container status (`docker compose ps`)
- **Ollama** – `GET http://localhost:11434/api/tags` (lists pulled models)
- **Model responsiveness** – `POST http://localhost:11434/api/generate` with a minimal prompt (`"Say OK"`) against one model (first listed, or `OLLAMA_MODEL` if set). Confirms the model can return a 200 and a response body (timeout 120s).
- **Temporal** – port `7233` open (workflow server)

**If Ollama is not reachable:**
1. Start containers: `docker compose up -d ollama`
2. From host, Ollama is at `http://localhost:11434`. From another container (e.g. worker), use `OLLAMA_BASE_URL=http://ollama:11434`
3. Pull at least one model: `docker exec -it <ollama_container_name> ollama run llama3.2:latest`

**Ollama healthcheck:** The `ollama` service in `docker-compose.yml` has a healthcheck (`ollama list`). Use `docker compose ps` to see `(healthy)` once Ollama is ready.
