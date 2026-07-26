-- ====================================================================
-- VectoPath Database Initialization Script (Test Environment)
-- ====================================================================

CREATE SCHEMA IF NOT EXISTS knowledge;
SET search_path TO knowledge, public;

DROP TABLE IF EXISTS knowledge.resource_group_permission CASCADE;
DROP TABLE IF EXISTS knowledge.vector_store CASCADE;
DROP TABLE IF EXISTS knowledge.resource CASCADE;
DROP TABLE IF EXISTS knowledge.folder_group CASCADE;
DROP TABLE IF EXISTS knowledge.folder CASCADE;

DROP INDEX IF EXISTS knowledge.vector_store_embedding_idx;
DROP INDEX IF EXISTS knowledge.resource_name_idx;
DROP INDEX IF EXISTS knowledge.resource_status_idx;
DROP INDEX IF EXISTS knowledge.resource_created_at_idx;
DROP INDEX IF EXISTS knowledge.resource_folder_idx;
DROP INDEX IF EXISTS knowledge.resource_group_permission_resource_idx;
DROP INDEX IF EXISTS knowledge.resource_group_permission_group_idx;
DROP INDEX IF EXISTS knowledge.resource_group_permission_permission_idx;
DROP INDEX IF EXISTS knowledge.folder_group_permission_folder_idx;
DROP INDEX IF EXISTS knowledge.folder_group_permission_group_idx;
DROP INDEX IF EXISTS knowledge.folder_group_permission_permission_idx;
DROP INDEX IF EXISTS knowledge.folder_path_idx;
DROP INDEX IF EXISTS idx_resource_created_by;
DROP INDEX IF EXISTS idx_resource_folder_id;
DROP INDEX IF EXISTS idx_folder_created_by;

-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS hstore;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE knowledge.folder
(
    id              UUID PRIMARY KEY,
    name            VARCHAR     NOT NULL,
    path            VARCHAR     NOT NULL,
    complete_path   VARCHAR     NOT NULL UNIQUE,
    created_by      VARCHAR,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE knowledge.folder_group
(
    folder_id     UUID NOT NULL REFERENCES knowledge.folder (id) ON DELETE CASCADE,
    group_id      UUID NOT NULL REFERENCES referential.groups (id) ON DELETE CASCADE,
    PRIMARY KEY (folder_id, group_id)
);

CREATE INDEX folder_group_folder_idx ON knowledge.folder_group (folder_id);
CREATE INDEX folder_group_group_idx ON knowledge.folder_group (group_id);

CREATE TABLE knowledge.resource
(
    id           uuid                  DEFAULT uuid_generate_v4() PRIMARY KEY,
    folder_id    UUID         REFERENCES knowledge.folder (id) ON DELETE SET NULL,
    name         varchar(255) NOT NULL,
    content      text         NOT NULL,
    content_type varchar(100),
    status       varchar(20)  NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'PROCESSING', 'VECTORIZED', 'ERROR')),
    metadata     jsonb,
    source_type  varchar(20),
    source_name  varchar(500),
    size         bigint,
    created_by   varchar,
    created_at   TIMESTAMPTZ           DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMPTZ           DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX resource_name_idx ON knowledge.resource (name);
CREATE INDEX resource_status_idx ON knowledge.resource (status);
CREATE INDEX resource_created_at_idx ON knowledge.resource (created_at DESC);
CREATE INDEX resource_folder_idx ON knowledge.resource (folder_id);

CREATE TABLE knowledge.resource_group_permission
(
    resource_id   UUID NOT NULL REFERENCES knowledge.resource (id) ON DELETE CASCADE,
    group_id      UUID NOT NULL REFERENCES referential.groups (id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES referential.permissions (id),
    PRIMARY KEY (resource_id, group_id)
);

CREATE INDEX resource_group_permission_resource_idx ON knowledge.resource_group_permission (resource_id);
CREATE INDEX resource_group_permission_group_idx ON knowledge.resource_group_permission (group_id);
CREATE INDEX resource_group_permission_permission_idx ON knowledge.resource_group_permission (permission_id);


-- ====================================================================
-- Table vector_store: Required by Spring AI Vector Store
-- ====================================================================
CREATE TABLE IF NOT EXISTS knowledge.vector_store
(
    id        text PRIMARY KEY,
    content   text NOT NULL,
    metadata  jsonb,
    embedding vector(1536)
);

-- HNSW index for optimized vector searches
CREATE INDEX IF NOT EXISTS vector_store_embedding_idx ON knowledge.vector_store
    USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);

-- Defaults data
INSERT INTO knowledge.folder (id, name, path, complete_path, created_by)
VALUES ('00000000-0000-0000-0000-000000000001', 'Root Folder', 'ROOT', 'ROOT', 'anonymous');

INSERT INTO knowledge.folder_group (folder_id, group_id)
VALUES ('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001');


CREATE INDEX idx_resource_created_by ON knowledge.resource(created_by);
CREATE INDEX idx_resource_folder_id ON knowledge.resource(folder_id);
CREATE INDEX idx_folder_created_by ON knowledge.folder(created_by);

