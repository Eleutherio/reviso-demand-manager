CREATE TABLE IF NOT EXISTS email_outbox (
  id UUID PRIMARY KEY,
  to_email VARCHAR(160) NOT NULL,
  subject VARCHAR(200) NOT NULL,
  body TEXT NOT NULL,
  status VARCHAR(20) NOT NULL,
  attempts INTEGER NOT NULL DEFAULT 0,
  next_attempt_at TIMESTAMPTZ NOT NULL,
  last_error TEXT,
  provider_id VARCHAR(100),
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_email_outbox_status_next_attempt
  ON email_outbox (status, next_attempt_at);
