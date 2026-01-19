-- V17: Stripe Webhook Events (idempotency)

CREATE TABLE stripe_webhook_events (
    event_id VARCHAR(100) PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_webhook_events_processed_at ON stripe_webhook_events(processed_at);

-- Add unique constraint on checkout session to prevent duplicate provisioning
ALTER TABLE subscriptions ADD COLUMN stripe_checkout_session_id VARCHAR(100);
CREATE UNIQUE INDEX idx_subscriptions_checkout_session ON subscriptions(stripe_checkout_session_id) WHERE stripe_checkout_session_id IS NOT NULL;
