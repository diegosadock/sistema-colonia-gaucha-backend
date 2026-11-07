CREATE TABLE roles (
    name VARCHAR(20) PRIMARY KEY
);

INSERT INTO roles (name) VALUES ('ADMIN'), ('STAFF');

CREATE TABLE app_users (
    id UUID PRIMARY KEY,
    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_app_users_email UNIQUE (email),
    CONSTRAINT ck_app_users_email_normalized CHECK (email = lower(btrim(email))),
    CONSTRAINT ck_app_users_email_not_blank CHECK (length(btrim(email)) > 0)
);

CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    role_name VARCHAR(20) NOT NULL REFERENCES roles(name) ON DELETE RESTRICT,
    PRIMARY KEY (user_id, role_name)
);

CREATE INDEX ix_user_roles_role_name ON user_roles(role_name);
