CREATE INDEX IF NOT EXISTS idx_requests_client_created ON requests (client_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_requests_status_created ON requests (status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_requests_assignee_status ON requests (assignee_id, status);
CREATE INDEX IF NOT EXISTS idx_requests_due_status ON requests (due_date, status);
CREATE INDEX IF NOT EXISTS idx_requests_type_created ON requests (type, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_request_events_request_created ON request_events (request_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_request_events_type_created ON request_events (event_type, created_at DESC);
