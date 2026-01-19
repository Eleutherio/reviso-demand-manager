-- V22: Add stripe_product_id to subscription_plans

-- Add stripe_product_id (Stripe Product ID for reference)
ALTER TABLE subscription_plans ADD COLUMN IF NOT EXISTS stripe_product_id VARCHAR(100);
