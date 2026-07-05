@echo off
REM Java REST API Template - Development Startup Script (Windows)

echo Java REST API Template - Development Setup
echo ==========================================

REM Check Java
java -version >nul 2>&1
if errorlevel 1 (
    echo Java is not installed. Please install JDK 17+ and try again.
    pause
    exit /b 1
)
echo Java found:
java -version

REM Pick a Gradle command: prefer the wrapper, fall back to a system gradle.
set "GRADLE="
if exist "gradlew.bat" (
    if not exist "gradle\wrapper\gradle-wrapper.jar" (
        where gradle >nul 2>&1
        if not errorlevel 1 (
            echo Generating Gradle wrapper jar...
            call gradle wrapper
        )
    )
    set "GRADLE=gradlew.bat"
) else (
    where gradle >nul 2>&1
    if not errorlevel 1 (
        set "GRADLE=gradle"
    )
)

if "%GRADLE%"=="" (
    echo Neither gradlew.bat ^(wrapper jar^) nor a system 'gradle' is available.
    echo Install Gradle ^(https://gradle.org^) or run 'gradle wrapper' once to bootstrap.
    pause
    exit /b 1
)

if not exist "logs" mkdir logs

echo.
echo Setup complete! Choose how to run:
echo 1. Development mode (bootRun)
echo 2. Production mode (build fat jar, then run it)
echo 3. Docker mode (docker compose up --build)
echo.
set /p choice="Enter choice (1-3): "

if "%choice%"=="1" (
    echo Starting in development mode (bootRun)...
    call %GRADLE% bootRun
) else if "%choice%"=="2" (
    echo Building and starting the production jar...
    call %GRADLE% clean bootJar
    java -jar build\libs\app.jar
) else if "%choice%"=="3" (
    echo Starting with Docker...
    docker compose up --build
) else (
    echo Invalid choice. Starting in development mode...
    call %GRADLE% bootRun
)

pause
