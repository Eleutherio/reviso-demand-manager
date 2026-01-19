-- V21: Pending Signups (Secure Password Storage)

CREATE TABLE pending_signups (
    id UUID PRIMARY KEY,
    checkout_session_id VARCHAR(100) UNIQUE NOT NULL,
    plan_id UUID NOT NULL REFERENCES subscription_plans(id),
    agency_name VARCHAR(160) NOT NULL,
    admin_email VARCHAR(160) NOT NULL,
    password_hash TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_pending_signups_checkout ON pending_signups(checkout_session_id);
CREATE INDEX idx_pending_signups_expires ON pending_signups(expires_at);

-- Cleanup job: delete expired signups (run periodically)
-- DELETE FROM pending_signups WHERE expires_at < now();
