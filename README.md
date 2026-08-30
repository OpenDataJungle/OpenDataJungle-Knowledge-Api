[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-25-orange.svg)](https://openjdk.org/projects/jdk/25/)
[![CI Pipeline](https://github.com/OpenDataJungle/OpenDataJungle-Knowledge-Api/actions/workflows/ci.yml/badge.svg)](https://github.com/OpenDataJungle/OpenDataJungle-Knowledge-Api/actions/workflows/ci.yml)

# OpenDataJungle Knowledge API

A REST API for resource management and semantic search. It stores and vectorizes text, URL, and file resources into
PostgreSQL with pgvector, organizes them into a folder hierarchy, and exposes a semantic search endpoint with optional
re-ranking.

Part of the [OpenDataJungle](https://www.opendatajungle.com) platform, alongside
the [Conversation API](https://github.com/OpenDataJungle/OpenDataJungle-Conversation-Api) which queries it for semantic
search and resource content.

## Architecture

Hexagonal architecture (Ports & Adapters): `client` (REST controllers/DTOs) → `business` (domain services/models) →
`infra` (persistence, embedding/reranker clients, external integrations), enforced by ArchUnit tests.

## Features

- **Resource management** — create, list, search, rename, reprocess, and delete text resources, with content sourced
  from raw text, a URL (downloaded and extracted with jsoup), or an uploaded file.
- **Processing status tracking** — resources move through `PENDING`, `PROCESSING`, `VECTORIZED`, and `ERROR`, with
  custom JSON metadata attached along the way.
- **Folder hierarchy** — organize resources in a tree of folders (`path` / `complete_path`), each user gets a default
  folder created on demand, and folder creation/update is gated by group write access.
- **Semantic search** — configurable vectorization via **OpenAI embeddings** or **Ollama** (local models), cosine
  similarity search in pgvector with an HNSW index, and optional **re-ranking** with HuggingFace TEI.
- **Group-based authorization** — resources and folders carry group permissions resolved against an external
  OpenDataJungle Reference Data API, layered on top of scope-based endpoint authorization.
- **OAuth2 / JWT security** — endpoints are protected with scope-based authorization (`resources.read`,
  `resources.write`, `resources.delete`, `folders.read`, `folders.write`, `folders.delete`, `search.semantic`).
- **Observability** — Actuator health/info/metrics endpoints with Prometheus support out of the box.

## Tech stack

| Component  | Technology                              |
|------------|-----------------------------------------|
| Framework  | Spring Boot 4                           |
| Language   | Java 25                                 |
| AI         | Spring AI 2 — OpenAI, Ollama embeddings |
| Database   | PostgreSQL with pgvector extension      |
| Re-ranking | HuggingFace TEI (via REST API)          |
| Security   | Spring Security, OAuth2 Resource Server |
| Testing    | JUnit 5, Testcontainers, ArchUnit       |
| Scraping   | jsoup                                   |
| Metrics    | Micrometer + Prometheus                 |
| API docs   | springdoc-openapi                       |

## Getting started

### Prerequisites

- JDK 25
- Docker (for the local PostgreSQL/pgvector database and integration tests)
- An OpenAI API key **or** a running Ollama instance (for embeddings)
- An OAuth2/OIDC provider issuing JWTs (e.g. Keycloak) for authentication — not required in `local`/`test` profile,
  see [Security](#security)
- An OpenDataJungle Reference Data API instance
- A HuggingFace TEI instance (optional, for re-ranking)

### Run the database

```bash
cd infra/container
docker compose up -d
```

This starts a PostgreSQL instance with the `pgvector` extension on port `5432`.

### Run the application

```bash
./mvnw spring-boot:run
```

The API starts on `http://localhost:8081` by default.

### Run the tests

```bash
# Unit tests
./mvnw test

# Integration tests (requires Docker for Testcontainers)
./mvnw verify -Pit
```

## Configuration

Configuration lives in `src/main/resources/application.yml` and is overridable via environment variables.

#### Application & server

| Variable              | Description         | Default                        |
|-----------------------|---------------------|--------------------------------|
| `APPLICATION_TITLE`   | Application title   | `OpenDataJungle Knowledge API` |
| `APPLICATION_VERSION` | Application version | `pom.xml` version              |
| `SERVER_PORT`         | HTTP port           | `8081`                         |

#### Database

| Variable                                | Description            | Default                                             |
|-----------------------------------------|------------------------|-----------------------------------------------------|
| `DATABASE_URL`                          | PostgreSQL JDBC URL    | `jdbc:postgresql://localhost:5432/open_data_jungle` |
| `DATABASE_USERNAME`                     | PostgreSQL user        | `user`                                              |
| `DATABASE_PASSWORD`                     | PostgreSQL password    | `password`                                          |
| `DATABASE_DRIVER`                       | JDBC driver            | `org.postgresql.Driver`                             |
| `JPA_DDL_AUTO`                          | Hibernate DDL mode     | `none`                                              |
| `JPA_SHOW_SQL` / `HIBERNATE_FORMAT_SQL` | Log/format SQL queries | `false`                                             |
| `JPA_OPEN_IN_VIEW`                      | Open Session In View   | `false`                                             |

#### Embedding

| Variable               | Description                                                  | Default                  |
|------------------------|--------------------------------------------------------------|--------------------------|
| `EMBEDDING_PROVIDER`   | Embedding provider — `OPENAI` or `OLLAMA`                    | `OPENAI`                 |
| `EMBEDDING_API_KEY`    | API key (required if provider = `OPENAI`)                    | `demo`                   |
| `EMBEDDING_BASE_URL`   | Base URL (for `OLLAMA`)                                      | `http://localhost:11434` |
| `EMBEDDING_MODEL`      | Embedding model                                              | `text-embedding-3-small` |
| `EMBEDDING_DIMENSIONS` | Vector dimensions (optional, inferred from model by default) | —                        |

#### pgvector

| Variable                           | Description                      | Default           |
|------------------------------------|----------------------------------|-------------------|
| `PGVECTOR_INDEX_TYPE`              | Index type — `HNSW` or `IVFFLAT` | `HNSW`            |
| `PGVECTOR_DISTANCE_TYPE`           | Distance type                    | `COSINE_DISTANCE` |
| `PGVECTOR_DIMENSIONS`              | Vector dimensions                | `1536`            |
| `PGVECTOR_MAX_DOCUMENT_BATCH_SIZE` | Maximum batch size               | `10000`           |

#### Re-ranking

| Variable            | Description                                          | Default                 |
|---------------------|------------------------------------------------------|-------------------------|
| `RERANKER_TYPE`     | Reranker type — e.g. `HUGGINGFACE`, empty to disable | —                       |
| `RERANKER_BASE_URL` | HuggingFace TEI instance URL                         | `http://localhost:8085` |

#### Security

| Variable                          | Description                  | Default                          |
|-----------------------------------|------------------------------|----------------------------------|
| `SECURITY_ADMIN_ROLE`             | Administrator role           | `resource.admin`                 |
| `SECURITY_NOT_AFFECTABLE_ROLES`   | Non-assignable roles         | `resource.admin,search.semantic` |
| `SECURITY_SCOPE_SEARCH_SEMANTIC`  | Scope for semantic search    | `search.semantic`                |
| `SECURITY_SCOPE_RESOURCES_READ`   | Scope for reading resources  | `resources.read`                 |
| `SECURITY_SCOPE_RESOURCES_WRITE`  | Scope for writing resources  | `resources.write`                |
| `SECURITY_SCOPE_RESOURCES_DELETE` | Scope for deleting resources | `resources.delete`               |
| `SECURITY_SCOPE_FOLDERS_READ`     | Scope for reading folders    | `folders.read`                   |
| `SECURITY_SCOPE_FOLDERS_WRITE`    | Scope for writing folders    | `folders.write`                  |
| `SECURITY_SCOPE_FOLDERS_DELETE`   | Scope for deleting folders   | `folders.delete`                 |

#### Reference Data API (groups & permissions)

Group membership and per-group permissions — used to authorize folder/resource creation and updates — are resolved by
calling an external OpenDataJungle Reference Data API, not managed by this service.

| Variable                                           | Description                                 | Default                        |
|----------------------------------------------------|---------------------------------------------|--------------------------------|
| `OPEN_DATA_JUNGLE_REFERENCE_DATA_API_BASE_URL`     | Base URL of the Reference Data API          | `http://localhost:8083/api/v1` |
| `OPEN_DATA_JUNGLE_REFERENCE_DATA_API_USER_GROUPS`  | Path template to list a user's groups       | `/users/{userId}/groups`       |
| `OPEN_DATA_JUNGLE_REFERENCE_DATA_API_USER_BY_NAME` | Path template to resolve a user by username | `/users/username/{username}`   |

#### Content download

| Variable                                   | Description                                                      | Default          |
|--------------------------------------------|------------------------------------------------------------------|------------------|
| `CONTENT_DOWNLOAD_TIMEOUT`                 | Download timeout in seconds                                      | `30`             |
| `CONTENT_DOWNLOAD_CONNECT_TIMEOUT`         | Connection timeout in seconds                                    | `10`             |
| `CONTENT_DOWNLOAD_MAX_SIZE_BYTES`          | Maximum size of downloaded content in bytes                      | `5242880` (5MB)  |
| `CONTENT_DOWNLOAD_BLOCK_INTERNAL_NETWORKS` | Block URL resources pointing to internal/private networks (SSRF) | `true`           |
| `CONTENT_DOWNLOAD_ALLOWED_HOSTS`           | Comma-separated host allowlist for URL resources                 | — (no allowlist) |

#### Uploads

| Variable                     | Description                                 | Default |
|------------------------------|---------------------------------------------|---------|
| `MULTIPART_MAX_FILE_SIZE`    | Maximum size of a single uploaded file      | `5MB`   |
| `MULTIPART_MAX_REQUEST_SIZE` | Maximum size of the whole multipart request | `10MB`  |

See `application.yml` for the full list.

> [!NOTE]
> Authentication (OAuth2/JWT), CORS, and error-handling are wired by the shared
> [`opendatajungle-commons`](https://github.com/OpenDataJungle/OpenDataJungle-Commons).<br>
> See its README for the corresponding configuration properties.

## Security

Endpoints require a JWT (OAuth2 Resource Server) with the scopes listed above.

Two Spring profiles disable authentication entirely and allow anonymous access—useful for standalone environments, local
development, and automated testing:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

- **`local`** — for running the API locally without an OAuth2 server.
- **`test`** — used automatically by the test suite (`@ActiveProfiles("test")`).

In production, no profile is active by default, so OAuth2/JWT authentication is enforced.

### Configuration examples

#### Minimal configuration (local, OpenAI)

```bash
export EMBEDDING_PROVIDER=OPENAI
export EMBEDDING_API_KEY=sk-your-api-key-here
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

#### Minimal configuration (local, Ollama)

```bash
export EMBEDDING_PROVIDER=OLLAMA
export EMBEDDING_BASE_URL=http://localhost:11434
export EMBEDDING_MODEL=nomic-embed-text
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

## API overview

All endpoints are under `/api/v1` and require a valid JWT with the appropriate scope (unless the `local`/`test`
profile is active, see [Security](#security)).

### Resources — `/api/v1/resources`

| Method   | Path                  | Scope              | Description                                                  |
|----------|-----------------------|--------------------|--------------------------------------------------------------|
| `POST`   | `/`                   | `resources.write`  | Create a resource (text, URL, or with content inline)        |
| `POST`   | `/upload`             | `resources.write`  | Create a resource from an uploaded file                      |
| `GET`    | `/`                   | `resources.read`   | List resources                                               |
| `GET`    | `/{id}`               | `resources.read`   | Get a resource                                               |
| `GET`    | `/search?name=&path=` | `resources.read`   | Search resources by name and/or path (at least one required) |
| `GET`    | `/status/{status}`    | `resources.read`   | Filter resources by status                                   |
| `GET`    | `/{id}/content`       | `resources.read`   | Retrieve a resource's raw content                            |
| `PATCH`  | `/{id}`               | `resources.write`  | Rename a resource                                            |
| `POST`   | `/{id}/reprocess`     | `resources.write`  | Reprocess (re-vectorize) a resource                          |
| `DELETE` | `/{id}`               | `resources.delete` | Delete a resource                                            |

### Folders — `/api/v1/folders`

| Method   | Path             | Scope            | Description                                     |
|----------|------------------|------------------|-------------------------------------------------|
| `POST`   | `/`              | `folders.write`  | Create a folder                                 |
| `GET`    | `/`              | `folders.read`   | List all folders                                |
| `POST`   | `/me`            | `folders.read`   | Get or create the current user's default folder |
| `GET`    | `/{id}`          | `folders.read`   | Get a folder                                    |
| `GET`    | `/{id}/children` | `folders.read`   | List a folder's direct children                 |
| `PUT`    | `/{id}`          | `folders.write`  | Update a folder                                 |
| `DELETE` | `/{id}`          | `folders.delete` | Delete a folder                                 |

### Search — `/api/v1/search`

| Method | Path        | Scope             | Description                               |
|--------|-------------|-------------------|-------------------------------------------|
| `POST` | `/semantic` | `search.semantic` | Semantic search over vectorized resources |

`POST /semantic` accepts `query` (required), `limit` (default `10`), `min_similarity` (default `0.5`), and an optional
`resource_ids` list to restrict the search.

The full OpenAPI 3 specification is available at [
`docs/OpenDataJungleKnowledgeAPI_Openapi.json`](docs/OpenDataJungleKnowledgeAPI_Openapi.json). It's regenerated from the
running application (`/v3/api-docs`) by the `OpenApiGenerationIT` integration test.

## Contributing

Issues and pull requests are welcome: https://github.com/OpenDataJungle/OpenDataJungle-Commons

## Contact

- **Website:** [www.opendatajungle.com](https://www.opendatajungle.com)
- **Email:** [contact@opendatajungle.com](mailto:contact@opendatajungle.com)
- **Organization:** [github.com/OpenDataJungle](https://github.com/OpenDataJungle)

## License

Licensed under the [GNU General Public License v3.0](LICENSE).
