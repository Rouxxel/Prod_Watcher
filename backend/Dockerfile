#############################################################################
### Multi-stage Dockerfile for the Java REST API Template
### Spring Boot 3 + Java 17 (Temurin). Optimized for production deployment.
#############################################################################

# ---- Build stage: compile and produce the fat jar ----
FROM eclipse-temurin:17-jdk AS builder

WORKDIR /app

# Copy Gradle wrapper + build scripts first to leverage layer caching.
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle

# Pre-fetch dependencies (cached unless build files change).
# `|| true` so the first run doesn't fail just because there are no sources yet.
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies || true

# Copy sources and build the jar (skip tests for a faster image build).
COPY src ./src
RUN ./gradlew --no-daemon clean bootJar -x test

# ---- Production stage: minimal JRE runtime ----
FROM eclipse-temurin:17-jre

# Create a non-root user for security.
RUN groupadd -r appuser && useradd -r -g appuser appuser

WORKDIR /app

# Copy only the built jar from the builder stage.
COPY --from=builder /app/build/libs/app.jar ./app.jar

# Create the logs directory and hand ownership to the non-root user.
RUN mkdir -p logs && chown -R appuser:appuser /app

USER appuser

# Health check hits the root endpoint.
HEALTHCHECK --interval=30s --timeout=10s --start-period=20s --retries=3 \
    CMD wget -qO- http://localhost:8080/ >/dev/null 2>&1 || exit 1

# Expose the configured port (config_file.json -> network.server_port).
EXPOSE 8080

# Run the application.
ENTRYPOINT ["java", "-jar", "app.jar"]
