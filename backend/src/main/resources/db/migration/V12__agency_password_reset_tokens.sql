CREATE TABLE agency_password_reset_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    email VARCHAR(160) NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_agency_password_reset_tokens_email_created_at
    ON agency_password_reset_tokens (email, created_at DESC);

CREATE INDEX idx_agency_password_reset_tokens_email_token_hash
    ON agency_password_reset_tokens (email, token_hash);
