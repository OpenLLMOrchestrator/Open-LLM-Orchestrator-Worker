# Build stage
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

COPY gradlew .
COPY build.gradle .
COPY settings.gradle .
COPY gradle gradle/

# Required subprojects (settings.gradle includes these). Do not remove – root build depends on them.
COPY plugin-contract plugin-contract/
COPY engine-config engine-config/
COPY engine-model engine-model/
# plugins/ must exist (e.g. plugins/README.md); put plugins/*.zip here to bundle them into the fat JAR
COPY plugins plugins/

# Fail fast if a subproject is missing (e.g. .dockerignore or context issue)
RUN for d in plugin-contract engine-config engine-model; do test -d /app/$d || (echo "Missing /app/$d - check Dockerfile COPY and .dockerignore" && exit 1); done

# Fix CRLF in gradlew (Windows) so Linux can run it (shebang must be #!/bin/sh not #!/bin/sh\r)
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew

RUN ./gradlew dependencies --no-daemon || true

COPY src src/
# Build fat JAR (shadowJar) so runtime is a single JAR with worker + plugin-contract + any plugin zips from plugins/
RUN ./gradlew unpackPluginZips shadowJar --no-daemon

# Run stage: single fat JAR (no installDist)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN adduser -D -u 1000 appuser

# Shared folder for plugins: mount a volume here so any plugin can read/write files from the host
# Override path with SHARED_FOLDER_PATH if needed (default /app/shared)
RUN mkdir -p /app/shared && chown appuser:appuser /app/shared
VOLUME /app/shared

COPY --from=build /app/build/libs/open-llm-orchestrator-worker-*-all.jar /app/worker.jar
COPY config config/

USER appuser

# Config: CONFIG_FILE_PATH unset => config/<CONFIG_KEY>.json (e.g. /app/config/default.json)
ENV CONFIG_FILE_PATH=/app/config/default.json
ENTRYPOINT ["java", "-jar", "/app/worker.jar"]
