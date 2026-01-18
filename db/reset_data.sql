-- Script para resetar o banco mantendo apenas Reviso Agency e admin@reviso

-- 1. Deletar eventos de demandas
DELETE FROM request_events;

-- 2. Deletar demandas
DELETE FROM requests;

-- 3. Deletar briefings
DELETE FROM briefings;

-- 4. Deletar tokens de reset de senha
DELETE FROM password_reset_tokens;

-- 5. Deletar outbox de emails
DELETE FROM email_outbox;

-- 6. Deletar usuários exceto admin@reviso
DELETE FROM users WHERE email != 'admin@reviso.com';

-- 7. Deletar empresas exceto Reviso Agency
DELETE FROM companies WHERE name != 'Reviso Agency';

-- 8. Deletar agências exceto a default (não precisa deletar, já está protegida pela empresa)

-- Mensagem de sucesso
DO $$
BEGIN
  RAISE NOTICE 'Banco resetado com sucesso!';
  RAISE NOTICE '';
  RAISE NOTICE 'Dados mantidos:';
  RAISE NOTICE '- Agencia: Reviso Agency';
  RAISE NOTICE '- Empresa: Reviso Agency';
  RAISE NOTICE '- Usuario: admin@reviso.com';
END $$;
