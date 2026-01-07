-- V5 - Update user_role enum to support granular RBAC

-- Drop and recreate the enum with new values
ALTER TABLE users ALTER COLUMN role DROP DEFAULT;
ALTER TABLE users ALTER COLUMN role TYPE VARCHAR(50);

DROP TYPE IF EXISTS user_role;

CREATE TYPE user_role AS ENUM ('AGENCY_ADMIN', 'AGENCY_USER', 'CLIENT_USER');

ALTER TABLE users ALTER COLUMN role TYPE user_role USING 
  CASE 
    WHEN role = 'AGENCY' THEN 'AGENCY_ADMIN'::user_role
    WHEN role = 'CLIENT' THEN 'CLIENT_USER'::user_role
    ELSE 'AGENCY_USER'::user_role
  END;

ALTER TABLE users ALTER COLUMN role SET DEFAULT 'CLIENT_USER';
