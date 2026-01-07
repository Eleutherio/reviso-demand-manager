-- Script para popular dados de teste no sistema B2B
-- Execute após as migrations

-- 1. Criar empresa da agência
INSERT INTO companies (id, name, company_type, active, created_at)
VALUES 
  ('11111111-1111-1111-1111-111111111111', 'Reviso Agency', 'AGENCY', true, now())
ON CONFLICT (id) DO NOTHING;

-- 2. Criar empresa cliente de exemplo
INSERT INTO companies (id, name, company_type, active, created_at)
VALUES 
  ('22222222-2222-2222-2222-222222222222', 'Cliente ABC Ltda', 'CLIENT', true, now())
ON CONFLICT (id) DO NOTHING;

-- 3. Criar usuário AGENCY_ADMIN
-- Senha: admin123 (hash bcrypt)
INSERT INTO users (id, full_name, email, password_hash, role, company_id, active, created_at)
VALUES (
  '33333333-3333-3333-3333-333333333333',
  'Admin Reviso',
  'admin@reviso.com',
  '$2a$10$N9qo8uLOickgx2ZMRZoMye.Jx3/kfRYPZCjQ3vZGZY.2K1zVvQmIy',
  'AGENCY_ADMIN',
  '11111111-1111-1111-1111-111111111111',
  true,
  now()
) ON CONFLICT (email) DO NOTHING;

-- 4. Criar usuário AGENCY_USER
-- Senha: designer123
INSERT INTO users (id, full_name, email, password_hash, role, company_id, active, created_at)
VALUES (
  '44444444-4444-4444-4444-444444444444',
  'João Designer',
  'joao@reviso.com',
  '$2a$10$8B7kXvZ.nQxP8Cz3Nj8W7eK9Qz5L.gKmJvRtYwXdNhPqEfGhIjKlM',
  'AGENCY_USER',
  '11111111-1111-1111-1111-111111111111',
  true,
  now()
) ON CONFLICT (email) DO NOTHING;

-- 5. Criar usuário CLIENT_USER
-- Senha: cliente123
INSERT INTO users (id, full_name, email, password_hash, role, company_id, active, created_at)
VALUES (
  '55555555-5555-5555-5555-555555555555',
  'Maria Silva',
  'maria@clienteabc.com',
  '$2a$10$7A6jWuY.mPwO7By2Mi7V6dJ8Py4K.fJlIuQsXwVcMgOnDeDfGhIjK',
  'CLIENT_USER',
  '22222222-2222-2222-2222-222222222222',
  true,
  now()
) ON CONFLICT (email) DO NOTHING;

-- 6. Criar briefings de exemplo
INSERT INTO briefings (id, company_id, created_by_user_id, title, description, status, created_at)
VALUES 
  (
    '66666666-6666-6666-6666-666666666666',
    '22222222-2222-2222-2222-222222222222',
    '55555555-5555-5555-5555-555555555555',
    'Campanha Black Friday 2026',
    'Precisamos de 15 artes para Instagram Stories, 5 posts para feed e 3 vídeos curtos para Reels promovendo ofertas de até 70% off.',
    'PENDING',
    now() - interval '2 hours'
  ),
  (
    '77777777-7777-7777-7777-777777777777',
    '22222222-2222-2222-2222-222222222222',
    '55555555-5555-5555-5555-555555555555',
    'Landing Page Novo Produto',
    'Criar landing page responsiva para lançamento do novo produto linha premium. Precisa ter formulário de captação de leads e integração com CRM.',
    'PENDING',
    now() - interval '1 day'
  )
ON CONFLICT (id) DO NOTHING;

-- 7. Criar demanda de exemplo (já convertida)
INSERT INTO briefings (id, company_id, created_by_user_id, title, description, status, created_at)
VALUES 
  (
    '88888888-8888-8888-8888-888888888888',
    '22222222-2222-2222-2222-222222222222',
    '55555555-5555-5555-5555-555555555555',
    'Email Marketing Semanal',
    'Template de email para newsletter semanal com novidades e promoções.',
    'CONVERTED',
    now() - interval '3 days'
  )
ON CONFLICT (id) DO NOTHING;

INSERT INTO requests (id, client_id, company_id, title, description, type, priority, status, assignee_id, due_date, revision_count, created_at, updated_at)
VALUES (
  '99999999-9999-9999-9999-999999999999',
  '22222222-2222-2222-2222-222222222222',
  '22222222-2222-2222-2222-222222222222',
  'Email Marketing Semanal',
  'Template de email para newsletter semanal com novidades e promoções.',
  'EMAIL',
  'MEDIUM',
  'IN_PROGRESS',
  '44444444-4444-4444-4444-444444444444',
  now() + interval '5 days',
  0,
  now() - interval '3 days',
  now() - interval '2 days'
) ON CONFLICT (id) DO NOTHING;

-- Mensagem de sucesso
DO $$ 
BEGIN 
  RAISE NOTICE '✅ Dados de teste criados com sucesso!';
  RAISE NOTICE '';
  RAISE NOTICE 'Usuários disponíveis:';
  RAISE NOTICE '1. admin@reviso.com / admin123 (AGENCY_ADMIN)';
  RAISE NOTICE '2. joao@reviso.com / designer123 (AGENCY_USER)';
  RAISE NOTICE '3. maria@clienteabc.com / cliente123 (CLIENT_USER)';
  RAISE NOTICE '';
  RAISE NOTICE 'Faça login em: POST http://localhost:8080/auth/login';
END $$;
