# OpenDataJungle Knowledge Api - Resource Manager with Vectorization

OpenDataJungle Knowledge Api is an intelligent resource management API with vectorization and semantic search capabilities
using PostgreSQL with the pgvector extension. The application integrates Spring Security with OAuth2 to secure resource
access and provides fine-grained role management to control permissions.

## Architecture

Hexagonal architecture (Ports & Adapters) with clear separation of concerns:

- **Business**: Business domain, models and use cases
- **Client**: Primary adapters (REST API, cross-cutting tools)
- **Infrastructure**: Secondary adapters (Database, configurations, technical services)

## Features

### Resource Management

- Creation and storage of textual resources
- Support for multiple source types:
    - **TEXT**: Creation from textual content
    - **URL**: Download and content extraction from a URL (with jsoup)
    - **FILE**: Import of text files
- Automatic content vectorization
- Processing status tracking (PENDING, PROCESSING, VECTORIZED, ERROR)
- Search by name and filtering by status
- Custom metadata management (JSON)
- Raw content retrieval for a resource
- Resource renaming
- Traceability with source fields (source_type, source_url, source_content_type)

### Folder Management

- Hierarchical folder tree (`path` / `complete_path`) to organize resources
- Per-user default folder (`POST /api/v1/folders/me`), created on demand
- Group-based write access control when creating or updating a folder

### Semantic Search

- Intelligent content chunking
- Configurable vectorization: **OpenAI Embeddings** or **Ollama** (local models)
- Cosine similarity search in pgvector with HNSW index
- Semantic search API with:
    - Configurable result limit (`limit`)
    - Minimum similarity threshold (`min_similarity`)
    - Filtering by resource identifiers (`resource_ids`)
- **Re-ranking** of results with HuggingFace TEI (optional)

### Observability

- Metrics exposed via **Micrometer** (Prometheus)
- Actuator endpoints: `health`, `info`, `metrics`, `prometheus`
- MDC for user traceability in logs

### Security

- OAuth2 authentication with JWT (scope-based endpoint authorization)
- Group-based permissions on resources and folders, checked against an external Reference Data API
- Customizable CORS configuration
- Local/test profile without security

## Technologies Used

| Component       | Technology                              |
|-----------------|-----------------------------------------|
| Framework       | Spring Boot **4**                       |
| Language        | Java **25**                             |
| Database        | PostgreSQL with pgvector extension      |
| AI / Embeddings | Spring AI **2**, OpenAI, Ollama         |
| Re-ranking      | HuggingFace TEI (via REST API)          |
| Security        | Spring Security, OAuth2 Resource Server |
| Testing         | JUnit 5, Testcontainers, ArchUnit       |
| Scraping        | jsoup                                   |
| Metrics         | Micrometer + Prometheus                 |
| Utilities       | Lombok                                  |

## Project Structure

```
src/main/java/com/opendatajungle/
├── business/               # Business layer (domain)
│   ├── model/             # Domain models
│   ├── repository/        # Ports (interfaces)
│   ├── service/           # Business services (use cases) / Ports
│   │   ├── impl/         # Business service implementations
│   │   └── splitter/     # Document splitting strategies
│   └── exception/         # Business exceptions
├── client/                # Client layer (primary adapters)
│   ├── config/           # Configuration (GlobalExceptionHandler, etc.)
│   ├── controller/        # REST controllers
│   ├── dto/              # DTOs for REST API
│   ├── exception/         # Client exceptions
│   ├── service/           # Resource creation orchestrators
│   └── tool/             # Cross-cutting tools (RequestTool)
├── infra/                 # Infrastructure layer (secondary adapters)
│   ├── conf/             # Configurations (Security, CORS, MDC, Embedding, Reranker)
│   ├── entity/            # JPA entities
│   ├── repository/        # Repository implementations
│   ├── service/           # Technical services (ContentDownload, Reranker, Splitter)
│   └── properties/        # Externalized properties (Embedding, Reranker, Security, CORS)
└── shared/                # Shared code (utilities)
```

## Quick Start

### Prerequisites

- Java 25
- Maven 3.x
- PostgreSQL with pgvector extension
- OpenAI API Key **or** Ollama instance (for embeddings)
- OAuth2 Server (production only, e.g. Keycloak)
- OpenDataJungle Reference Data API instance (optional, required only for group-based permissions)
- HuggingFace TEI instance (optional, for re-ranking)

### Getting Started

#### 1. PostgreSQL Database with pgvector

Start PostgreSQL with Docker Compose (includes pgvector extension):

```bash
cd infra/container
docker-compose up -d
```

The database will be accessible at:

- Host: `localhost:5432`
- Database: `open_data_jungle`
- User: `user`
- Password: `password`

To verify the container is running:

```bash
docker ps | grep open-data-jungle-pgvector-db
```

To stop the database:

```bash
docker-compose down
```

#### 2. Environment Variables Configuration

Minimum configuration (OpenAI):

```bash
export EMBEDDING_PROVIDER=OPENAI
export EMBEDDING_API_KEY=sk-your-api-key-here
export EMBEDDING_MODEL=text-embedding-3-small
```

Configuration with Ollama (local model):

```bash
export EMBEDDING_PROVIDER=OLLAMA
export EMBEDDING_BASE_URL=http://localhost:11434
export EMBEDDING_MODEL=nomic-embed-text
```

#### 3. Starting the Application

```bash
# Local mode (without OAuth2 security)
./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# Production mode (with OAuth2 security)
./mvnw spring-boot:run
```

The API is available at `http://localhost:8081`

## API Endpoints

### Resources

#### Create a resource (direct text)

```http
POST /api/v1/resources
Content-Type: application/json

{
  "name": "Example document",
  "content": "Here is the content of my document...",
  "source_type": "TEXT",
  "metadata": "{\"source\":\"upload\",\"author\":\"user\"}",
  "folder_id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "group_permissions": [
    { "group_id": "b3f1c2d4-1234-4567-8901-abcdef123456", "permission_id": "c4a2e6f8-1234-4567-8901-abcdef654321" }
  ]
}
```

`folder_id` and `group_permissions` are both optional. If `folder_id` is omitted, the resource is created in the current user's default folder (see [Folders](#folders)).

#### Create a resource from a URL

```http
POST /api/v1/resources
Content-Type: application/json

{
  "name": "Wikipedia Article",
  "source_type": "URL",
  "url": "https://en.wikipedia.org/wiki/Artificial_intelligence"
}
```

#### Upload a file

```http
POST /api/v1/resources/upload
Content-Type: multipart/form-data

file: [file]
name: "My document"
metadata: "{\"category\":\"documentation\"}"
folder_id: "3fa85f64-5717-4562-b3fc-2c963f66afa6"
group_permissions (JSON part): [{"group_id": "b3f1c2d4-1234-4567-8901-abcdef123456", "permission_id": "c4a2e6f8-1234-4567-8901-abcdef654321"}]
```

#### List all resources

```http
GET /api/v1/resources
```

#### Retrieve a resource

```http
GET /api/v1/resources/{id}
```

#### Retrieve raw content of a resource

```http
GET /api/v1/resources/{id}/content
```

#### Search resources by name

```http
GET /api/v1/resources/search?name=example
```

#### Filter by status

```http
GET /api/v1/resources/status/VECTORIZED
```

Available statuses:

- `PENDING`: Waiting for processing
- `PROCESSING`: Being vectorized
- `VECTORIZED`: Successfully vectorized
- `ERROR`: Error during processing
- `DELETED`: Deleted

#### Rename a resource

```http
PATCH /api/v1/resources/{id}
Content-Type: application/json

{
  "name": "New name"
}
```

#### Reprocess a resource

```http
POST /api/v1/resources/{id}/reprocess
```

#### Delete a resource

```http
DELETE /api/v1/resources/{id}
```

### Folders

Folders form a hierarchical tree: `path` is the complete path of the **parent** folder, and the folder's own `complete_path` is computed as `path + "/" + name`. Creating or updating a folder requires write access to the parent folder (and to the target groups, if `group_ids` is set).

#### Create a folder

```http
POST /api/v1/folders
Content-Type: application/json

{
  "name": "Reports",
  "path": "ROOT",
  "group_ids": ["b3f1c2d4-1234-4567-8901-abcdef123456"]
}
```

`group_ids` is optional: if omitted, the folder inherits the groups of its parent folder.

#### List all folders

```http
GET /api/v1/folders
```

#### Get or create the current user's default folder

```http
POST /api/v1/folders/me
```

#### Retrieve a folder

```http
GET /api/v1/folders/{id}
```

#### List a folder's direct children

```http
GET /api/v1/folders/{id}/children
```

#### Update a folder

```http
PUT /api/v1/folders/{id}
Content-Type: application/json

{
  "name": "Reports 2024",
  "path": "ROOT"
}
```

#### Delete a folder

```http
DELETE /api/v1/folders/{id}
```

### Semantic Search

#### Semantic search

```http
POST /api/v1/search/semantic
Content-Type: application/json

{
  "query": "How does vectorization work?",
  "limit": 10,
  "min_similarity": 0.5,
  "resource_ids": ["uuid-1", "uuid-2"]
}
```

| Parameter        | Type         | Default | Description                                          |
|------------------|--------------|---------|------------------------------------------------------|
| `query`          | `String`     | —       | Query text to search **(required)**                  |
| `limit`          | `Integer`    | `10`    | Maximum number of results returned                   |
| `min_similarity` | `Double`     | `0.5`   | Minimum similarity threshold (between 0 and 1)       |
| `resource_ids`   | `List<UUID>` | `null`  | Restrict the search to specific resource identifiers |

## Configuration

### Environment Variables

#### Embedding (Required)

- `EMBEDDING_PROVIDER`: Embedding provider — `OPENAI` (default) or `OLLAMA`
- `EMBEDDING_API_KEY`: API key (for OpenAI) **(REQUIRED if provider = OPENAI)**
- `EMBEDDING_BASE_URL`: Base URL (for Ollama, default: `http://localhost:11434`)
- `EMBEDDING_MODEL`: Embedding model (default: `text-embedding-3-small`)
- `EMBEDDING_DIMENSIONS`: Vector dimensions (optional, inferred from model by default)

#### Re-ranking (Optional)

- `RERANKER_TYPE`: Reranker type — `HUGGINGFACE` for example
- `RERANKER_BASE_URL`: HuggingFace TEI instance URL (default: `http://localhost:8085`)

#### Database

- `DATABASE_URL`: PostgreSQL URL (default: `jdbc:postgresql://localhost:5432/open_data_jungle`)
- `DATABASE_USERNAME`: PostgreSQL user (default: `user`)
- `DATABASE_PASSWORD`: PostgreSQL password (default: `password`)
- `DATABASE_DRIVER`: JDBC driver (default: `org.postgresql.Driver`)

#### JPA/Hibernate

- `JPA_DDL_AUTO`: DDL mode (default: `none`)
- `JPA_SHOW_SQL`: Show SQL queries (default: `false`)
- `HIBERNATE_FORMAT_SQL`: Format SQL queries (default: `false`)
- `JPA_OPEN_IN_VIEW`: Open Session In View (default: `false`)

#### OAuth2 Security

- `JWT_ISSUER_URI`: OAuth2 server URI (default: `http://localhost:8090/realms/master`)
- `SECURITY_ADMIN_ROLE`: Administrator role (default: `resource.admin`)
- `SECURITY_NOT_AFFECTABLE_ROLES`: Non-assignable roles (default: `resource.admin,search.semantic`)
- `SECURITY_SCOPE_SEARCH_SEMANTIC`: Scope for semantic search (default: `search.semantic`)
- `SECURITY_SCOPE_RESOURCES_READ`: Scope for reading resources (default: `resources.read`)
- `SECURITY_SCOPE_RESOURCES_WRITE`: Scope for writing resources (default: `resources.write`)
- `SECURITY_SCOPE_RESOURCES_DELETE`: Scope for deleting resources (default: `resources.delete`)
- `SECURITY_SCOPE_FOLDERS_READ`: Scope for reading folders (default: `folders.read`)
- `SECURITY_SCOPE_FOLDERS_WRITE`: Scope for writing folders (default: `folders.write`)
- `SECURITY_SCOPE_FOLDERS_DELETE`: Scope for deleting folders (default: `folders.delete`)

#### Reference Data API (Groups & Permissions)

Group membership and per-group permissions — used to authorize folder/resource creation and updates — are resolved by calling an external OpenDataJungle Reference Data API, not managed by this service.

- `OPEN_DATA_JUNGLE_REFERENCE_DATA_API_BASE_URL`: Base URL of the Reference Data API (default: `http://localhost:8083/api/v1`)
- `OPEN_DATA_JUNGLE_REFERENCE_DATA_API_USER_GROUPS`: Path template to list a user's groups (default: `/users/{userId}/groups`)
- `OPEN_DATA_JUNGLE_REFERENCE_DATA_API_USER_BY_NAME`: Path template to resolve a user by username (default: `/users/username/{username}`)

#### Uploads

- `MULTIPART_MAX_FILE_SIZE`: Maximum size of a single uploaded file (default: `5MB`)
- `MULTIPART_MAX_REQUEST_SIZE`: Maximum size of the whole multipart request (default: `10MB`)

#### PGVector

- `PGVECTOR_INDEX_TYPE`: Index type (default: `HNSW`) — possible values: `HNSW`, `IVFFLAT`
- `PGVECTOR_DISTANCE_TYPE`: Distance type (default: `COSINE_DISTANCE`)
- `PGVECTOR_DIMENSIONS`: Vector dimensions (default: `1536`)
- `PGVECTOR_MAX_DOCUMENT_BATCH_SIZE`: Maximum batch size (default: `10000`)

#### CORS

- `CORS_ALLOWED_ORIGINS`: Allowed origins (default:
  `http://localhost,http://localhost:3000,http://localhost:4200,http://localhost:8080,http://localhost:9000`)
- `CORS_ALLOWED_METHODS`: Allowed HTTP methods (default: `GET,POST,PUT,PATCH,DELETE,OPTIONS`)
- `CORS_ALLOWED_HEADERS`: Allowed headers (default: `Authorization,Content-Type,X-Requested-With,Accept,Origin,Access-Control-Request-Method,Access-Control-Request-Headers`)
- `CORS_EXPOSED_HEADERS`: Headers exposed to the client (default: `Access-Control-Allow-Origin,Access-Control-Allow-Credentials`)
- `CORS_ALLOW_CREDENTIALS`: Allow credentials (default: `false`)
- `CORS_MAX_AGE`: Preflight cache duration in seconds (default: `3600`)

#### Content

- `CONTENT_DOWNLOAD_TIMEOUT`: Download timeout in seconds (default: `30`)
- `CONTENT_DOWNLOAD_CONNECT_TIMEOUT`: Connection timeout in seconds (default: `10`)
- `CONTENT_DOWNLOAD_MAX_SIZE_BYTES`: Maximum size of downloaded content in bytes (default: `5242880`, i.e. 5MB)
- `CONTENT_DOWNLOAD_BLOCK_INTERNAL_NETWORKS`: Block URL resources pointing to internal/private networks, to prevent SSRF (default: `true`)
- `CONTENT_DOWNLOAD_ALLOWED_HOSTS`: Comma-separated host allowlist for URL resources (default: empty, i.e. no allowlist restriction beyond the internal-network block)

#### Application

- `APPLICATION_TITLE`: Application title (default: `OpenDataJungle Knowledge API`)
- `APPLICATION_VERSION`: Application version (default: pom.xml version)
- `SERVER_PORT`: Server port (default: `8081`)

#### Logging

- `LOGGING_LEVEL_OPENDATAJUNGLE`: Log level for OpenDataJungle (default: `INFO`)
- `LOGGING_LEVEL_SPRING_AI`: Log level for Spring AI (default: `INFO`)
- `LOGGING_PATTERN_CONSOLE`: Console log pattern
- `JACKSON_TIME_ZONE`: Jackson timezone (default: `UTC`)

### Configuration Examples

#### Minimal Configuration (Local, OpenAI)

```bash
export EMBEDDING_PROVIDER=OPENAI
export EMBEDDING_API_KEY=sk-your-api-key-here
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

#### Minimal Configuration (Local, Ollama)

```bash
export EMBEDDING_PROVIDER=OLLAMA
export EMBEDDING_BASE_URL=http://localhost:11434
export EMBEDDING_MODEL=nomic-embed-text
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

#### Production Configuration

```bash
# Embedding
export EMBEDDING_PROVIDER=OPENAI
export EMBEDDING_API_KEY=sk-your-api-key-here
export EMBEDDING_MODEL=text-embedding-3-small

# OAuth2 (Keycloak)
export JWT_ISSUER_URI=https://laulem.com/realms/production

# Database
export DATABASE_URL=jdbc:postgresql://db.myapp.com:5432/opendatajungle_prod
export DATABASE_USERNAME=opendatajungle_user
export DATABASE_PASSWORD=secure_password

# CORS
export CORS_ALLOWED_ORIGINS=https://localhost,https://www.laulem.com
export CORS_ALLOW_CREDENTIALS=true

# HuggingFace TEI Reranker (optional)
export RERANKER_TYPE=HUGGINGFACE
export RERANKER_BASE_URL=http://reranker.laulem.com:8085

./mvnw spring-boot:run
```

### Customization

- **Embedding provider**: `EMBEDDING_PROVIDER=OPENAI` or `EMBEDDING_PROVIDER=OLLAMA`
- **Embedding model**: `EMBEDDING_MODEL` (e.g. `text-embedding-3-small`, `nomic-embed-text`)
- **Search limit**: Adjust the `limit` parameter in API requests
- **pgvector index type**: HNSW (recommended) or IVFFLAT via `PGVECTOR_INDEX_TYPE`
- **Re-ranking**: Enable `RERANKER_TYPE=HUGGINGFACE` and point to a HuggingFace TEI instance. Empty if not reranking is desired.

## Security

### Authentication and Authorization

OpenDataJungle uses Spring Security with OAuth2 Resource Server (JWT) to secure resource access.

#### Authorization Model

Access control has two layers:

1. **Scope-based endpoint authorization**: each endpoint requires a JWT scope (see the `SECURITY_SCOPE_*` variables in [Configuration](#oauth2-security)), e.g. `resources.write` to create a resource, `folders.read` to list folders.
2. **Group-based object authorization**: group membership and permissions are not managed by this service — they are resolved from an external Reference Data API.
    - Resources carry a `group_permissions` list (`group_id` + `permission_id` pairs) for fine-grained access.
    - Folders carry a `group_ids` list; creating or updating a folder requires write access to the parent folder's groups.

#### Protected Endpoints

- `/actuator/health`, `/actuator/health/**`: Public access
- `/actuator/info`, `/actuator/metrics`, `/actuator/prometheus`: Authentication required
- `/api/v1/**`: Authentication required
- Other endpoints are protected by default

### Security Profiles

- **Production**: OAuth2 security enabled (default profile)
- **Local/Test**: Security disabled (`local` and `test` profiles)

## Development

### Testing

```bash
./mvnw test
```

Tests include:

- Architectural tests with ArchUnit (hexagonal architecture validation)
- REST controller tests

Integration tests relying on Testcontainers (Docker) are named `*IT.java` and are excluded from
the commands above. They only run via the `it` Maven profile (requires Docker access, so it
may not work in every devcontainer setup):

```bash
./mvnw verify -Pit
```

### Build

```bash
./mvnw clean package
```

### Docker

```bash
cd infra/container
docker-compose up -d
```

## TODO

- [ ] Paginated data retrieval
- [ ] Allow database authentication (in addition to OAuth2)
- [ ] Provide Swagger / OpenAPI
- [ ] Extend supported file types for vectorization
- [ ] Provide a sample Bruno collection
- [ ] Add more integration & unit tests
- [ ] Parametrize rerank multiplier (if rerank activated: RERANKER_TYPE not empty)
- [ ] Provide a Dockerfile to package the API as a container image


TODO : Lier les pré actions sans passer par un compteur de priorité
