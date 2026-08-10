CREATE SCHEMA IF NOT EXISTS reference_data;
SET search_path TO reference_data, public;

DROP TABLE IF EXISTS reference_data.group_users;

DROP TABLE IF EXISTS reference_data.permissions;
DROP TABLE IF EXISTS reference_data.groups;
DROP TABLE IF EXISTS reference_data.users;

DROP INDEX IF EXISTS reference_data.idx_group_users_group_id;

CREATE TABLE reference_data.users (
                                      id UUID PRIMARY KEY,
                                      first_name VARCHAR NOT NULL,
                                      last_name VARCHAR NOT NULL,
                                      username VARCHAR NOT NULL UNIQUE,
                                      created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                      updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE reference_data.groups (
                                       id UUID PRIMARY KEY,
                                       name VARCHAR NOT NULL UNIQUE,
                                       description TEXT,
                                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                       updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE reference_data.permissions (
                                            id UUID PRIMARY KEY,
                                            name VARCHAR NOT NULL UNIQUE,
                                            description TEXT,
                                            can_read BOOLEAN NOT NULL DEFAULT FALSE,
                                            can_write BOOLEAN NOT NULL DEFAULT FALSE,
                                            is_admin BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE reference_data.group_users (
                                            group_id UUID NOT NULL REFERENCES reference_data.groups(id) ON DELETE CASCADE,
                                            user_id UUID NOT NULL REFERENCES reference_data.users(id) ON DELETE CASCADE,
                                            permission_id UUID NOT NULL REFERENCES reference_data.permissions(id),
                                            PRIMARY KEY (group_id, user_id)
);


-- Default user and group (default user : anonymous) and root group (default group : root)
INSERT INTO reference_data.users (id, first_name, last_name, username)
VALUES ('00000000-0000-0000-0000-000000000001', 'Anonymous', 'User', 'anonymous');

INSERT INTO reference_data.groups (id, name, description)
VALUES ('00000000-0000-0000-0000-000000000001', 'root', 'Root group with all permissions');

-- Permission
INSERT INTO reference_data.permissions (id, name, description, can_read, can_write, is_admin)
VALUES ('00000000-0000-0000-0000-000000000001', 'root_permission', 'Root permission with all access rights', TRUE, TRUE, TRUE);

INSERT INTO reference_data.group_users (group_id, user_id, permission_id)
VALUES ('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001');

CREATE INDEX reference_data.idx_group_users_group_id ON reference_data.group_users(group_id);
