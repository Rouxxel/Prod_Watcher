#!/bin/bash

# Java REST API Template - Development Startup Script

echo "Java REST API Template - Development Setup"
echo "=========================================="

# Check Java
if ! command -v java &> /dev/null; then
    echo "Java is not installed. Please install JDK 17+ and try again."
    exit 1
fi
echo "Java found: $(java -version 2>&1 | head -n 1)"

# Pick a Gradle command: prefer the wrapper, fall back to a system gradle.
if [ -f "./gradlew" ]; then
    # Ensure the wrapper jar exists; regenerate it if a system gradle is available.
    if [ ! -f "gradle/wrapper/gradle-wrapper.jar" ] && command -v gradle &> /dev/null; then
        echo "Generating Gradle wrapper jar..."
        gradle wrapper
    fi
    chmod +x ./gradlew
    GRADLE="./gradlew"
elif command -v gradle &> /dev/null; then
    GRADLE="gradle"
else
    echo "Neither ./gradlew (wrapper jar) nor a system 'gradle' is available."
    echo "Install Gradle (https://gradle.org) or run 'gradle wrapper' once to bootstrap."
    exit 1
fi

# Load .env if present so API_* / E_* vars are exported for the run.
if [ -f ".env" ]; then
    echo "Loading environment from .env"
    set -a
    # shellcheck disable=SC1091
    source .env
    set +a
fi

mkdir -p logs

echo ""
echo "Setup complete! Choose how to run:"
echo "1. Development mode (bootRun, live restart with Spring DevTools if added)"
echo "2. Production mode (build fat jar, then run it)"
echo "3. Docker mode (docker compose up --build)"
echo ""
read -p "Enter choice (1-3): " choice

case $choice in
    1)
        echo "Starting in development mode (bootRun)..."
        $GRADLE bootRun
        ;;
    2)
        echo "Building and starting the production jar..."
        $GRADLE clean bootJar
        java -jar build/libs/app.jar
        ;;
    3)
        echo "Starting with Docker..."
        docker compose up --build
        ;;
    *)
        echo "Invalid choice. Starting in development mode..."
        $GRADLE bootRun
        ;;
esac
