-- V24 - Perfis de acesso e permissoes
CREATE TABLE IF NOT EXISTS access_profiles (
    id UUID PRIMARY KEY,
    agency_id UUID NOT NULL REFERENCES agencies(id),
    name VARCHAR(120) NOT NULL,
    description TEXT,
    is_default BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_access_profiles_agency_name
    ON access_profiles (agency_id, name);

CREATE INDEX IF NOT EXISTS idx_access_profiles_agency_id
    ON access_profiles (agency_id);

CREATE TABLE IF NOT EXISTS access_profile_permissions (
    profile_id UUID NOT NULL REFERENCES access_profiles(id) ON DELETE CASCADE,
    permission VARCHAR(120) NOT NULL,
    PRIMARY KEY (profile_id, permission)
);

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS access_profile_id UUID REFERENCES access_profiles(id);

CREATE INDEX IF NOT EXISTS idx_users_access_profile_id
    ON users (access_profile_id);
