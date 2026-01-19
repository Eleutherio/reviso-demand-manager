-- V16: Subscription Plans and Subscriptions

CREATE TABLE subscription_plans (
    id UUID PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    stripe_price_id VARCHAR(100) UNIQUE,
    max_users INTEGER,
    max_requests_per_month INTEGER,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE subscriptions (
    id UUID PRIMARY KEY,
    agency_id UUID NOT NULL REFERENCES agencies(id),
    plan_id UUID NOT NULL REFERENCES subscription_plans(id),
    stripe_subscription_id VARCHAR(100) UNIQUE,
    stripe_customer_id VARCHAR(100),
    status VARCHAR(20) NOT NULL,
    current_period_start TIMESTAMPTZ,
    current_period_end TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ
);

CREATE INDEX idx_subscriptions_agency ON subscriptions(agency_id);
CREATE INDEX idx_subscriptions_stripe_sub ON subscriptions(stripe_subscription_id);

-- Seed basic plans (Stripe Price IDs must be configured)
INSERT INTO subscription_plans (id, code, name, stripe_price_id, max_users, max_requests_per_month, active, created_at)
VALUES
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'STARTER', 'Starter', 'price_starter_placeholder', 5, 50, true, now()),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'PROFESSIONAL', 'Professional', 'price_professional_placeholder', 20, 200, true, now()),
    ('cccccccc-cccc-cccc-cccc-cccccccccccc', 'ENTERPRISE', 'Enterprise', 'price_enterprise_placeholder', NULL, NULL, true, now());
