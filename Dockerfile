# Build stage
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

COPY gradlew .
COPY build.gradle .
COPY gradle gradle/

RUN chmod +x gradlew && ./gradlew dependencies --no-daemon || true

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
# Default config path inside container (override with CONFIG_FILE_PATH)
ENV CONFIG_FILE_PATH=/app/config/engine-config.json

ENTRYPOINT ["worker"]
