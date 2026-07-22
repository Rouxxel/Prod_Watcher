#############################################################################
### Multi-stage Dockerfile for ProdWatch API
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

# Container-aware JVM tuning. Defaults are conservative so the app survives on
# small (e.g. 512 MB) hosts like Render's free/starter tier without being
# OOM-killed mid-startup. Override by setting JAVA_OPTS in the environment.
#   MaxRAMPercentage  - cap heap relative to the container memory limit
#   UseSerialGC       - lowest memory/CPU overhead for small heaps
#   TieredStopAtLevel - faster, lighter startup (less JIT/code cache)
#   Xss512k           - smaller thread stacks -> lower total RSS
#   ExitOnOutOfMemoryError - fail fast so the platform restarts cleanly
#   egd=urandom       - avoid blocking on entropy during secure-random init
ENV JAVA_OPTS="-XX:MaxRAMPercentage=70.0 -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Xss512k -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"

# Health check hits the root endpoint. Long start-period covers slow cold
# starts (Flyway validation + Spring context) so the container isn't marked
# unhealthy before it finishes booting.
HEALTHCHECK --interval=30s --timeout=10s --start-period=90s --retries=3 \
    CMD wget -qO- http://localhost:8080/ >/dev/null 2>&1 || exit 1

# Expose the configured port (config_file.json -> network.server_port).
EXPOSE 8080

# Run the application. Shell form + exec so JAVA_OPTS is expanded and java
# still becomes PID 1 (receives SIGTERM for graceful shutdown).
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
