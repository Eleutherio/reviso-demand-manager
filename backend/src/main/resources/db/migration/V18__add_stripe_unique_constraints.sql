-- Add UNIQUE constraints for Stripe IDs (1:1 relationships)
ALTER TABLE subscriptions ADD CONSTRAINT uk_stripe_subscription_id UNIQUE (stripe_subscription_id);
ALTER TABLE subscriptions ADD CONSTRAINT uk_stripe_checkout_session_id UNIQUE (stripe_checkout_session_id);

-- Add index for customer_id (1:N relationship)
CREATE INDEX idx_stripe_customer_id ON subscriptions(stripe_customer_id);
