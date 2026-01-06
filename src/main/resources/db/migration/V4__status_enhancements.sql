-- Add new values to request_status enum for richer workflow
DO $$ BEGIN
  ALTER TYPE request_status ADD VALUE IF NOT EXISTS 'DONE';
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
  ALTER TYPE request_status ADD VALUE IF NOT EXISTS 'CANCELED';
EXCEPTION WHEN duplicate_object THEN NULL; END $$;
