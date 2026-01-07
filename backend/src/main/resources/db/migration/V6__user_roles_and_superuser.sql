-- Ajusta enum de roles para casar com a aplicação e cria superusuário padrão

-- Habilita pgcrypto para gerar hashes bcrypt
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Renomeia enum antigo, cria enum novo com valores esperados e migra a coluna
DO $$
BEGIN
    ALTER TYPE user_role RENAME TO user_role_old;
EXCEPTION WHEN undefined_object THEN
    NULL;
END $$;

DO $$
BEGIN
    CREATE TYPE user_role AS ENUM ('AGENCY_ADMIN','AGENCY_USER','CLIENT_USER');
EXCEPTION WHEN duplicate_object THEN
    NULL;
END $$;

DO $$
BEGIN
    ALTER TABLE users ALTER COLUMN role DROP DEFAULT;
    ALTER TABLE users
    ALTER COLUMN role TYPE user_role
    USING (
        CASE role::text
            WHEN 'AGENCY' THEN 'AGENCY_ADMIN'
            WHEN 'CLIENT' THEN 'CLIENT_USER'
            ELSE role::text
        END::user_role
    );
    ALTER TABLE users ALTER COLUMN role SET DEFAULT 'CLIENT_USER';
EXCEPTION WHEN undefined_table THEN
    NULL;
END $$;

DO $$
BEGIN
    DROP TYPE IF EXISTS user_role_old;
EXCEPTION WHEN undefined_object THEN
    NULL;
END $$;

-- Garante empresa da agência
INSERT INTO companies (id, name, company_type, active, created_at)
VALUES (
    '11111111-1111-1111-1111-111111111111',
    'Reviso Agency',
    'AGENCY',
    true,
    now()
) ON CONFLICT (id) DO NOTHING;

-- Garante empresa cliente demo
INSERT INTO companies (id, name, company_type, active, created_at)
VALUES (
    '22222222-2222-2222-2222-222222222222',
    'Cliente ABC',
    'CLIENT',
    true,
    now()
) ON CONFLICT (id) DO NOTHING;

-- Cria/atualiza superusuário administrativo
INSERT INTO users (id, full_name, email, password_hash, role, company_id, active, created_at)
VALUES (
    '33333333-3333-3333-3333-333333333333',
    'Admin Reviso',
    'admin@reviso.com',
    crypt('admin123', gen_salt('bf', 10)),
    'AGENCY_ADMIN',
    '11111111-1111-1111-1111-111111111111',
    true,
    now()
)
ON CONFLICT (email) DO UPDATE
SET password_hash = EXCLUDED.password_hash,
    role = EXCLUDED.role,
    active = true,
    company_id = EXCLUDED.company_id;

-- Usuários auxiliares para acesso rápido (opcionais)
INSERT INTO users (id, full_name, email, password_hash, role, company_id, active, created_at)
VALUES
    (
        '44444444-4444-4444-4444-444444444444',
        'João Designer',
        'joao@reviso.com',
        crypt('designer123', gen_salt('bf', 10)),
        'AGENCY_USER',
        '11111111-1111-1111-1111-111111111111',
        true,
        now()
    ),
    (
        '55555555-5555-5555-5555-555555555555',
        'Maria Silva',
        'maria@clienteabc.com',
        crypt('cliente123', gen_salt('bf', 10)),
        'CLIENT_USER',
        '22222222-2222-2222-2222-222222222222',
        true,
        now()
    )
ON CONFLICT (email) DO NOTHING;