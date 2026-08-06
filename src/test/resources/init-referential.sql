CREATE SCHEMA IF NOT EXISTS referential;
SET search_path TO referential, public;

DROP TABLE IF EXISTS referential.group_users;

DROP TABLE IF EXISTS referential.permissions;
DROP TABLE IF EXISTS referential.groups;
DROP TABLE IF EXISTS referential.users;

DROP INDEX IF EXISTS idx_group_users_group_id;

CREATE TABLE referential.users (
                       id UUID PRIMARY KEY,
                       first_name VARCHAR NOT NULL,
                       last_name VARCHAR NOT NULL,
                       username VARCHAR NOT NULL UNIQUE,
                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE referential.groups (
                        id UUID PRIMARY KEY,
                        name VARCHAR NOT NULL UNIQUE,
                        description TEXT,
                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE referential.permissions (
                                         id UUID PRIMARY KEY,
                                         name VARCHAR NOT NULL UNIQUE,
                                         description TEXT,
                                         can_read BOOLEAN NOT NULL DEFAULT FALSE,
                                         can_write BOOLEAN NOT NULL DEFAULT FALSE,
                                         is_admin BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE referential.group_users (
                             group_id UUID NOT NULL REFERENCES referential.groups(id) ON DELETE CASCADE,
                             user_id UUID NOT NULL REFERENCES referential.users(id) ON DELETE CASCADE,
                             permission_id UUID NOT NULL REFERENCES referential.permissions(id),
                             PRIMARY KEY (group_id, user_id)
);


-- Default user and group (default user : anonymous) and root group (default group : root)
INSERT INTO referential.users (id, first_name, last_name, username)
VALUES ('00000000-0000-0000-0000-000000000001', 'Anonymous', 'User', 'anonymous');

INSERT INTO referential.groups (id, name, description)
VALUES ('00000000-0000-0000-0000-000000000001', 'root', 'Root group with all permissions');

-- Permission
INSERT INTO referential.permissions (id, name, description, can_read, can_write, is_admin)
VALUES ('00000000-0000-0000-0000-000000000001', 'root_permission', 'Root permission with all access rights', TRUE, TRUE, TRUE);

INSERT INTO referential.group_users (group_id, user_id, permission_id)
VALUES ('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001');

CREATE INDEX idx_group_users_group_id ON referential.group_users(group_id);
