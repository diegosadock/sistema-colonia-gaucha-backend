CREATE TABLE refresh_sessions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL,
    family_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMPTZ NOT NULL,
    rotated_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ,
    replaced_by_id UUID REFERENCES refresh_sessions(id) ON DELETE SET NULL,
    reuse_detected_at TIMESTAMPTZ,
    CONSTRAINT uk_refresh_sessions_token_hash UNIQUE (token_hash),
    CONSTRAINT ck_refresh_session_expiry CHECK (expires_at > created_at)
);

CREATE INDEX ix_refresh_sessions_family ON refresh_sessions(family_id);
CREATE INDEX ix_refresh_sessions_user_active ON refresh_sessions(user_id, expires_at) WHERE revoked_at IS NULL;
