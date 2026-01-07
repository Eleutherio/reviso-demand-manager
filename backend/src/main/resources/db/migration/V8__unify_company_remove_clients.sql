-- V8 - Unify domain: Company is the client; remove legacy clients/projects tables and request.client_id

-- 1) Migrate legacy clients -> companies (type CLIENT) using the same UUIDs
DO $$
BEGIN
  IF to_regclass('public.clients') IS NOT NULL THEN
    INSERT INTO companies (id, name, company_type, active, created_at, segment)
    SELECT c.id, c.name, 'CLIENT', c.active, c.created_at, c.segment
    FROM clients c
    WHERE NOT EXISTS (SELECT 1 FROM companies co WHERE co.id = c.id);
  END IF;
END $$;

-- 2) Ensure all requests have company_id (copy from legacy client_id when needed)
DO $$
BEGIN
  IF EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_name = 'requests'
      AND column_name = 'client_id'
  ) THEN
    UPDATE requests
    SET company_id = client_id
    WHERE company_id IS NULL;
  END IF;
END $$;

-- 3) company_id is the owner of the request
ALTER TABLE requests
  ALTER COLUMN company_id SET NOT NULL;

-- 4) Drop legacy indexes/constraints/columns/tables related to clients/projects
DROP INDEX IF EXISTS idx_requests_client_created;

ALTER TABLE requests
  DROP CONSTRAINT IF EXISTS requests_client_id_fkey,
  DROP CONSTRAINT IF EXISTS requests_project_id_fkey;

ALTER TABLE requests
  DROP COLUMN IF EXISTS client_id,
  DROP COLUMN IF EXISTS project_id;

DO $$
BEGIN
  IF to_regclass('public.projects') IS NOT NULL THEN
    ALTER TABLE projects
      DROP CONSTRAINT IF EXISTS projects_client_id_fkey;
  END IF;
END $$;

DROP TABLE IF EXISTS projects;
DROP TABLE IF EXISTS clients;
