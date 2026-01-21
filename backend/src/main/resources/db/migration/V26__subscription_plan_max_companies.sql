-- V26 - Limite de empresas por plano
ALTER TABLE subscription_plans
    ADD COLUMN IF NOT EXISTS max_companies INTEGER;

UPDATE subscription_plans
SET max_companies = max_users
WHERE max_companies IS NULL
  AND max_users IS NOT NULL;
