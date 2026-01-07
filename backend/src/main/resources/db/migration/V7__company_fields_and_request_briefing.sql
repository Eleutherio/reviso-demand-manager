-- V7 - Add extra fields to companies and link requests to briefings

-- Companies: metadata fields described in the diagram
ALTER TABLE companies
    ADD COLUMN IF NOT EXISTS segment VARCHAR(160),
    ADD COLUMN IF NOT EXISTS contact_email VARCHAR(160),
    ADD COLUMN IF NOT EXISTS site VARCHAR(300),
    ADD COLUMN IF NOT EXISTS useful_links JSONB;

-- Requests: optional link to the originating briefing
ALTER TABLE requests
    ADD COLUMN IF NOT EXISTS briefing_id UUID REFERENCES briefings(id);

CREATE INDEX IF NOT EXISTS idx_requests_briefing_id ON requests (briefing_id);
