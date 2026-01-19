-- V20: Provisioning Constraints (Concurrency Protection)

-- Email único global (não por tenant)
CREATE UNIQUE INDEX idx_users_email_unique ON users(LOWER(email));

-- Company code único por agência
CREATE UNIQUE INDEX idx_companies_code_agency ON companies(company_code, agency_id);

-- Stripe IDs únicos (já existem, mas garantir)
CREATE UNIQUE INDEX IF NOT EXISTS idx_subscriptions_stripe_sub ON subscriptions(stripe_subscription_id) WHERE stripe_subscription_id IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_subscriptions_stripe_checkout ON subscriptions(stripe_checkout_session_id) WHERE stripe_checkout_session_id IS NOT NULL;

-- Agency name único (opcional, mas recomendado)
CREATE UNIQUE INDEX idx_agencies_name_unique ON agencies(LOWER(name));
