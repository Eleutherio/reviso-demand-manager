DO $$ BEGIN
  CREATE TYPE request_status AS ENUM ('NEW','IN_PROGRESS','IN_REVIEW','CHANGES_REQUESTED','APPROVED','DELIVERED','CLOSED');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
  CREATE TYPE request_priority AS ENUM ('LOW','MEDIUM','HIGH','URGENT');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
  CREATE TYPE request_type AS ENUM ('POST','STORY','BANNER','LANDING_PAGE','TRAFFIC','EMAIL','VIDEO','OTHER');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
  CREATE TYPE request_event_type AS ENUM ('REQUEST_CREATED','ASSIGNED','STATUS_CHANGED','COMMENT_ADDED','REVISION_ADDED','DUE_DATE_CHANGED','PRIORITY_CHANGED');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

CREATE TABLE IF NOT EXISTS clients (
  id UUID PRIMARY KEY,
  name VARCHAR(120) NOT NULL,
  segment VARCHAR(80),
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS users (
  id UUID PRIMARY KEY,
  full_name VARCHAR(120) NOT NULL,
  email VARCHAR(160) NOT NULL UNIQUE,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS projects (
  id UUID PRIMARY KEY,
  client_id UUID NOT NULL REFERENCES clients(id),
  name VARCHAR(140) NOT NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS requests (
  id UUID PRIMARY KEY,
  client_id UUID NOT NULL REFERENCES clients(id),
  project_id UUID REFERENCES projects(id),

  title VARCHAR(140) NOT NULL,
  description TEXT,

  type request_type NOT NULL DEFAULT 'OTHER',
  priority request_priority NOT NULL DEFAULT 'MEDIUM',
  status request_status NOT NULL DEFAULT 'NEW',

  requester_name VARCHAR(120),
  requester_email VARCHAR(160),

  assignee_id UUID REFERENCES users(id),

  due_date TIMESTAMPTZ,
  revision_count INT NOT NULL DEFAULT 0,

  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

  CONSTRAINT ck_requester_email_format
    CHECK (requester_email IS NULL OR requester_email LIKE '%_@_%._%')
);

CREATE TABLE IF NOT EXISTS request_events (
  id UUID PRIMARY KEY,
  request_id UUID NOT NULL REFERENCES requests(id) ON DELETE CASCADE,
  actor_id UUID REFERENCES users(id),

  event_type request_event_type NOT NULL,

  from_status request_status,
  to_status request_status,

  message TEXT,
  revision_number INT,

  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

  CONSTRAINT ck_status_change_pair
    CHECK ((event_type <> 'STATUS_CHANGED') OR (from_status IS NOT NULL AND to_status IS NOT NULL))
);

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_requests_updated_at ON requests;
CREATE TRIGGER trg_requests_updated_at
BEFORE UPDATE ON requests
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();
