# Java REST API Template

A production-ready **Spring Boot 3 (Java 17)** template for building scalable REST APIs with Docker support, config-driven rate limiting, a custom logger, input validation, and RSA encryption utilities.

The concepts map one-to-one; the structure follows idiomatic Java backend conventions (`controller` / `service` / `repository` / `entity` / `dto` / `error`) instead of being a literal 1:1 port.

## Features

- **Spring Boot 3 + Java 17**: modern, fast web framework (the FastAPI equivalent).
- **Central specifications (`core_specs/`)**: a single `config_file.json` drives ports, logging, rate limits, and every endpoint's path/tag — read both programmatically and as `${config....}` placeholders in annotations.
- **Config-driven rate limiting**: per-endpoint limits from `config_file.json`, enforced by an interceptor + a shared in-memory limiter (no Redis required).
- **Custom logger**: thread-safe file + console logger with daily rotation (`CustomLogger`, used everywhere instead of `System.out`).
- **Validation**: Jakarta Bean Validation on DTOs **plus** reusable `Validators` helpers (email/password/phone/token/UUID).
- **RSA encryption utilities**: `EnDeCrypt` (OAEP SHA-256) + `KeysGenerator`, pure JDK.
- **Secure file I/O**: hardened atomic read/write with path confinement (`SecureFileIo`).
- **Docker**: multi-stage `Dockerfile` (non-root user) + `docker-compose.yml`.
- **OpenAPI / Swagger UI**: interactive docs out of the box.
- **Health checks**: root endpoint + Spring Actuator.

## Project Structure

```
java_rest_api_template/
├── src/
│   ├── main/
│   │   ├── java/com/template/restapi/
│   │   │   ├── Application.java                 # Entry point — "just run" this (boots logger, then Spring)
│   │   │   ├── config/
│   │   │   │   ├── CoreSpecsInitializer.java     # Bridges config_file.json/general_data.json → Spring Environment
│   │   │   │   ├── RateLimit.java                # @RateLimit("<endpoint_key>") annotation
│   │   │   │   ├── RateLimitInterceptor.java     # Enforces @RateLimit using ConfigLoader + RateLimiter
│   │   │   │   ├── WebConfig.java                # Registers the interceptor
│   │   │   │   ├── BeansConfig.java              # Shared beans (the RateLimiter instance)
│   │   │   │   └── StartupConfigurer.java        # Wires Validators from config at startup
│   │   │   ├── core_specs/                       # Central specifications
│   │   │   │   ├── configuration/ConfigLoader.java   # Loads config_file.json → ConfigLoader.get()
│   │   │   │   └── data/DataLoader.java              # Loads general_data.json → DataLoader.get()
│   │   │   ├── controller/                       # API routes (FastAPI routers equivalent)
│   │   │   │   ├── RootController.java               # Root / health-check endpoint
│   │   │   │   ├── example_group_one/
│   │   │   │   │   └── ExampleItemsController.java   # Reference controller — copy this pattern (full CRUD)
│   │   │   │   └── example_group_two/
│   │   │   │       └── ExampleStatusController.java  # Second group (own folder, own config entry)
│   │   │   ├── service/ExampleItemService.java   # Business logic
│   │   │   ├── repository/ExampleItemRepository.java # Persistence (in-memory; swap for JPA/file)
│   │   │   ├── entity/ExampleItem.java           # Persisted record
│   │   │   ├── dto/                              # Request/response shapes (Pydantic models equivalent)
│   │   │   │   ├── ExampleItemCreate.java            # POST body
│   │   │   │   ├── ExampleItemUpdate.java            # PATCH body (all fields optional)
│   │   │   │   └── ExampleItemResponse.java          # API response shape
│   │   │   ├── error/                            # Exceptions + global handler (request_limiter equivalent)
│   │   │   │   ├── GlobalExceptionHandler.java       # Maps exceptions → JSON (429/400/404/500)
│   │   │   │   ├── RateLimitExceededException.java   # → 429
│   │   │   │   ├── ResourceNotFoundException.java    # → 404
│   │   │   │   └── ErrorResponse.java                # Uniform error body
│   │   │   └── util/                             # Reusable utilities (mirrors src/utils/)
│   │   │       ├── CustomLogger.java                 # log handler — use everywhere
│   │   │       ├── RateLimiter.java                  # shared limiter instance
│   │   │       ├── Validators.java                   # email/password/phone/token/UUID validators
│   │   │       ├── EnDeCrypt.java                    # RSA encrypt/decrypt (keys from env)
│   │   │       ├── KeysGenerator.java                # one-off RSA key-pair generator
│   │   │       ├── SecureFileIo.java                 # hardened atomic file read/write
│   │   │       └── LogsDeleter.java                  # dev cleanup of logs/build folders
│   │   └── resources/
│   │       ├── application.properties           # Tiny bootstrap (registers the initializer)
│   │       ├── core_specs/
│   │       │   ├── configuration/config_file.json   # Endpoint, network, logging, rate-limit settings
│   │       │   └── data/general_data.json           # Static reference data
│   │       └── db/                              # DB assets (placeholder)
│   │           ├── .gitkeep                          # Migration files (Flyway/Liquibase) go here
│   │           └── mock_db_jsons/.gitkeep            # Mock JSON tables go here
│   └── test/java/com/template/restapi/
│       └── ApplicationTests.java               # Context-load smoke test
├── logs/                                        # Log files (created automatically)
├── build.gradle / settings.gradle              # Build config + dependencies (requirements.txt equivalent)
├── gradlew / gradlew.bat / gradle/wrapper/      # Gradle wrapper
├── Dockerfile                                   # Multi-stage Docker build
├── docker-compose.yml
├── .env / .env.example                          # Environment variables template
├── start.sh / start.bat                         # Dev setup and launch scripts
├── .dockerignore
├── .gitignore
└── README.md
```

## Quick Start

### Option 1: Run with Gradle (Development)

```bash
cd whatever_you_renamed_the_folder_to

# Linux/macOS
./gradlew bootRun

# Windows
gradlew.bat bootRun

# Windows alt
.\gradlew bootRun
```

Or use the helper scripts which also load `.env` and offer dev/prod/Docker modes:

```bash
./start.sh          # Linux/macOS
start.bat           # Windows
```

> **Gradle wrapper note:** this template ships `gradle/wrapper/gradle-wrapper.jar`. If your copy is missing it (some zip exports strip jars), bootstrap it once with a system Gradle (`gradle wrapper`) or just open the project in IntelliJ/VS Code, which generates it automatically. `start.sh` / `start.bat` fall back to a system `gradle` if the wrapper jar is absent.

Then access:
- API: http://localhost:8080
- Swagger UI: http://localhost:8080/docs
- OpenAPI JSON: http://localhost:8080/api-docs
- Actuator health: http://localhost:8080/actuator/health

### Option 2: Run the built jar (Production)

```bash
./gradlew clean bootJar
java -jar build/libs/app.jar
```

### Option 3: Run with Docker

```bash
docker compose up --build
```

Or manually:

```bash
docker build -t java-rest-api-template .
docker run -p 8080:8080 --env-file .env java-rest-api-template
```

## Configuration

### Central specifications — `core_specs/`

`ConfigLoader` parses `config_file.json` once at startup and exposes it as a `JsonNode` tree:

```java
import com.template.restapi.core_specs.configuration.ConfigLoader;

// Java:
String route = ConfigLoader.endpoint("example_endpoint_1").path("endpoint_route").asText();
```

In addition, `CoreSpecsInitializer` flattens the same JSON into the Spring Environment **before** the context starts, so the values can be referenced with `${...}` placeholders inside annotations:

```java
@RestController
@RequestMapping("${config.endpoints.example_endpoint_1.endpoint_prefix}")  // -> /subsection
public class ExampleItemsController {

    @RateLimit("example_endpoint_1")
    @GetMapping("${config.endpoints.example_endpoint_1.endpoint_route}")    // -> /items
    public List<ExampleItemResponse> list() { ... }
}
```

This is how a Spring controller stays "config-driven" the way a FastAPI router pulls its prefix/route from `config_loader`.

**Sections in `config_file.json`:**

| Section | Purpose |
|---|---|
| `defaults` | Shared paths and defaults |
| `logging` | Log level, directory, file-name prefix (read by `CustomLogger`) |
| `email_validation` | Allowed email providers and TLDs (applied to `Validators` at startup) |
| `network` | Bound `server_port`, `host`, optional `context_path` (mapped onto Spring's `server.*`) |
| `endpoints` | Per-endpoint prefix, tag, route, and rate-limit settings |

**Endpoint config shape** (one entry per route group):

```json
"example_endpoint_1": {
    "request_limit": 10,
    "unit_of_time_for_limit": "m",
    "endpoint_prefix": "/subsection",
    "endpoint_tag": "example-items",
    "endpoint_route": "/items"
}
```

`unit_of_time_for_limit` accepts `s` (second), `m` (minute), `h` (hour), `d` (day).

### Static data — `general_data.json` + `DataLoader`

`DataLoader` works like `ConfigLoader` but loads `core_specs/data/general_data.json`. Use it for reference data that is not environment-specific (supported languages, lookup tables):

```java
import com.template.restapi.core_specs.data.DataLoader;

List<String> languages = DataLoader.stringList("languages");
```

### Environment variables (`.env`)

```bash
# Used by docker-compose to publish the port (the actual bound port comes from config_file.json)
SERVER_PORT=8080

# API metadata (read by application.properties)
API_TITLE=Java REST API Template
API_VERSION=1.0.0
API_DESCRIPTION=A template for building REST APIs with Spring Boot

# RSA keys (only needed if you use util/EnDeCrypt) — generate with util/KeysGenerator
E_PUBLIC_KEY=your_public_key_pem_here
E_PRIVATE_KEY=your_private_key_pem_here
E_PRIVATE_PASSWORD=optional_unused_with_bundled_keygen
```

> The bound server port/host come from `config_file.json` (`network.*`), which `CoreSpecsInitializer` maps onto Spring's `server.port` / `server.address`. `SERVER_PORT` in `.env` is only used by `docker-compose` for port publishing.

## Adding New Endpoints

Use `controller/example_group_one/ExampleItemsController.java` as the canonical reference. It demonstrates every pattern the template expects:

1. **Config-driven** — prefix/route via `${config....}` placeholders, rate limit via `@RateLimit`.
2. **DTOs** — `ExampleItemCreate` / `ExampleItemUpdate` / `ExampleItemResponse` (`dto/`).
3. **Validation** — `@Valid` bean validation on the body **and** a `Validators` call on a query param.
4. **Service/repository** — controller delegates to `ExampleItemService`, which uses `ExampleItemRepository`.
5. **Static data** — reads `languages` from `DataLoader`.
6. **Logging** — `CustomLogger` for debug/info messages.

**Steps to add a new endpoint group:**

1. Add an entry under `endpoints` in `config_file.json`.
2. Create a new package under `controller/<your_group>/` and a controller (copy the reference). Keep **one resource group per package** so the codebase stays organized instead of one endless file.
3. Add the matching `dto/` records (follow the Create / Update / Response split) and, if persisted, an `entity/` + `repository/` + `service/`.
4. Annotate each handler with `@RateLimit("<your_endpoint_key>")`.

Spring auto-discovers the new `@RestController` via component scanning — there is no central registration step.

**Example endpoints shipped with the template:**

| Method | Path | Description |
|---|---|---|
| `GET` | `/` | Health check |
| `GET` | `/subsection/items` | List example items |
| `POST` | `/subsection/items` | Create an example item (optional `?contactEmail=`) |
| `GET` | `/subsection/items/{id}` | Get a single item by ID |
| `PATCH` | `/subsection/items/{id}` | Partially update an item |
| `DELETE` | `/subsection/items/{id}` | Delete an item |
| `GET` | `/subsection/status` | Second-group status endpoint |

## Models / Layers (`dto/`, `entity/`)

| Class | Role
|---|---|---|
| `dto/*Create` | POST body (no server-managed `id`)
| `dto/*Update` | PATCH body (all fields optional)
| `dto/*Response` | API response shape (decoupled from storage)
| `entity/*` | The full persisted record incl. `id`

Rename or replace these per project; keep the split.

## Utilities (`util/`)

| Class | When to use
|---|---|---|
| `CustomLogger` | Use everywhere instead of `System.out`; set up once in `Application.main`
| `RateLimiter` | Shared in-memory limiter instance
| `Validators` | `validateEmailFormat`, `validatePasswordFormat`, `validatePhoneFormat`, token/UUID checks
| `EnDeCrypt` | `encryptIn` / `decryptOut` for RSA-encrypted fields (keys from env)
| `KeysGenerator` | Run once to generate `E_PRIVATE_KEY` / `E_PUBLIC_KEY`
| `SecureFileIo` | Safe atomic file reads/writes with path confinement; JSON record helpers
| `LogsDeleter` | Dev cleanup — set `ROOT_FOLDER` before running

The 429 rate-limit response is handled by `error/GlobalExceptionHandler` + `RateLimitExceededException` in this template.

### Generating RSA keys for `EnDeCrypt`

```bash
./gradlew classes   # compile once so the class exists
java -cp build/classes/java/main com.template.restapi.util.KeysGenerator
# -> writes private_rsa_key.pem and public_rsa_key.pem
```

Paste the PEM contents into `E_PUBLIC_KEY` / `E_PRIVATE_KEY` in `.env` (newlines may be `\n`-escaped on one line). The bundled generator produces an **unencrypted** PKCS#8 private key (pure-JDK limitation); `E_PRIVATE_PASSWORD` is accepted but ignored. See the note in `EnDeCrypt.java` if you need an encrypted key (add BouncyCastle).

## Rate Limiting — how it works

1. `@RateLimit("example_endpoint_1")` marks a handler with a `config_file.json` endpoint key.
2. `RateLimitInterceptor` reads `request_limit` + `unit_of_time_for_limit` for that key from `ConfigLoader`.
3. The shared `RateLimiter` tracks a fixed window per `(client IP + endpoint key)`.
4. On exceed, it throws `RateLimitExceededException`, which `GlobalExceptionHandler` turns into **HTTP 429**.

The store is in-memory, which is correct for a single instance. For multi-instance deployments, replace the `ConcurrentHashMap` in `RateLimiter` with a Redis-backed counter (see **Scaling** below).

## Logging

- **File logging**: timestamped log files in `logs/`, daily rotation (UTC date change).
- **Console logging**: structured single-line output for containers.
- **Configurable level**: `logging.logging_level` in `config_file.json` (`debug`/`info`/`warning`/`error`/`critical`).
- **Initialization**: `Application.main` calls `CustomLogger.setup(...)` before the context boots; a JVM shutdown hook flushes/closes it.

## Security Features

- **Config-driven rate limiting** per endpoint.
- **Input validation**: DTO bean validation + `Validators` helpers.
- **RSA encryption**: `EnDeCrypt` (keys from environment, never committed).
- **Non-root Docker user**.
- **Path-confined file I/O**: `SecureFileIo.setAllowedRoot(...)` blocks `../` and symlink escapes.
- **Secrets via `.env`**; `*.pem` is git-ignored.

## Docker

### Multi-stage build
- **Builder stage**: Temurin JDK builds the fat jar (`app.jar`).
- **Production stage**: minimal Temurin JRE, non-root user, health check.

### Docker Compose
- Publishes `${SERVER_PORT:-8080}:8080`, persists `./logs`.
- Commented-out `postgres` / `redis` services ready to enable.

## Development

### Hot restart
Add Spring DevTools for automatic restart on recompile:

```groovy
developmentOnly 'org.springframework.boot:spring-boot-devtools'
```

### Adding dependencies
Edit `build.gradle` `dependencies { ... }` and re-run `./gradlew build`. Spring's dependency management plugin manages versions for Spring/3rd-party starters.

### Running tests

```bash
./gradlew test
```

## Scaling / Production notes

- **Rate limiter**: swap the in-memory store in `RateLimiter` for Redis (e.g. Bucket4j + Redis) for multi-instance correctness.
- **Persistence**: replace `ExampleItemRepository` with `JpaRepository<ExampleItem, String>` (add `spring-boot-starter-data-jpa` + a driver), or back it with `SecureFileIo` record helpers against `resources/db/mock_db_jsons/`.
- **Migrations**: add Flyway/Liquibase and put scripts in `src/main/resources/db/`.
- Compatible with AWS ECS/Fargate, Google Cloud Run, Azure Container Instances, Heroku, DigitalOcean App Platform, and similar.

## Requirements

- JDK 17+ (tested with 17 and 21)
- Gradle (the wrapper handles this; a system Gradle is only needed to bootstrap a missing wrapper jar)
- Docker / Docker Compose (optional)

## Customization

### Changing the package name
The base package is `com.template.restapi`. Rename it (IDE refactor) and update:
- `group` in `build.gradle`
- `context.initializer.classes` in `application.properties`
- the `package` line in `util/` files (these are copy-into-project helpers)

### Changing the API title / description
Set `API_TITLE`, `API_VERSION`, `API_DESCRIPTION` in `.env`.

### Adding authentication
Add `spring-boot-starter-security`, create a security config under `config/`, and protect controllers with method/URL security.

## License

This template is provided as-is for educational and development purposes.
