-- V14 - Add agency_id to core tables
ALTER TABLE companies
    ADD COLUMN IF NOT EXISTS agency_id UUID REFERENCES agencies(id);

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS agency_id UUID REFERENCES agencies(id);

ALTER TABLE requests
    ADD COLUMN IF NOT EXISTS agency_id UUID REFERENCES agencies(id);

ALTER TABLE briefings
    ADD COLUMN IF NOT EXISTS agency_id UUID REFERENCES agencies(id);

CREATE INDEX IF NOT EXISTS idx_companies_agency_id ON companies (agency_id);
CREATE INDEX IF NOT EXISTS idx_users_agency_id ON users (agency_id);
CREATE INDEX IF NOT EXISTS idx_requests_agency_id ON requests (agency_id);
CREATE INDEX IF NOT EXISTS idx_briefings_agency_id ON briefings (agency_id);
