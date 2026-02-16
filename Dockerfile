# Build stage
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

COPY gradlew .
COPY build.gradle .
COPY settings.gradle .
COPY gradle gradle/

# Subprojects required by root build.gradle (project(':plugin-contract'), project(':plugins'))
COPY plugin-contract plugin-contract/
COPY plugins plugins/

# Fix CRLF in gradlew (Windows) so Linux can run it (shebang must be #!/bin/sh not #!/bin/sh\r)
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew

RUN ./gradlew dependencies --no-daemon || true

COPY src src/
RUN ./gradlew installDist --no-daemon

# Run stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN adduser -D -u 1000 appuser

COPY --from=build /app/build/install/worker /app/install
COPY config config/

USER appuser

ENV PATH="/app/install/bin:${PATH}"
# Config file: when CONFIG_FILE_PATH is unset, path is config/<CONFIG_KEY>.json (e.g. /app/config/default.json)

ENTRYPOINT ["worker"]
