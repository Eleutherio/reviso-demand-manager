-- V3 - Add multi-tenant support (companies, users with auth, briefings)

DO $$ BEGIN
  CREATE TYPE company_type AS ENUM ('AGENCY','CLIENT');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
  CREATE TYPE user_role AS ENUM ('AGENCY','CLIENT');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
  CREATE TYPE briefing_status AS ENUM ('PENDING','CONVERTED','REJECTED');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

-- Create companies table
CREATE TABLE IF NOT EXISTS companies (
  id UUID PRIMARY KEY,
  name VARCHAR(160) NOT NULL,
  company_type company_type NOT NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Add fields to users table
ALTER TABLE users
ADD COLUMN IF NOT EXISTS password_hash VARCHAR NOT NULL DEFAULT '',
ADD COLUMN IF NOT EXISTS company_id UUID REFERENCES companies(id),
ADD COLUMN IF NOT EXISTS role user_role DEFAULT 'CLIENT';

-- Create briefings table
CREATE TABLE IF NOT EXISTS briefings (
  id UUID PRIMARY KEY,
  company_id UUID NOT NULL REFERENCES companies(id),
  created_by_user_id UUID REFERENCES users(id),
  title VARCHAR(160) NOT NULL,
  description TEXT,
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Add company_id to requests (nullable initially, will be required for new requests)
ALTER TABLE requests
ADD COLUMN IF NOT EXISTS company_id UUID REFERENCES companies(id);

-- Add visible_to_client to request_events
ALTER TABLE request_events
ADD COLUMN IF NOT EXISTS visible_to_client BOOLEAN NOT NULL DEFAULT true;

-- Create indexes for new fields
CREATE INDEX IF NOT EXISTS idx_companies_type ON companies (company_type);
CREATE INDEX IF NOT EXISTS idx_users_company_role ON users (company_id, role);
CREATE INDEX IF NOT EXISTS idx_briefings_company_created ON briefings (company_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_briefings_status ON briefings (status);
CREATE INDEX IF NOT EXISTS idx_requests_company_created ON requests (company_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_request_events_visible ON request_events (visible_to_client);
