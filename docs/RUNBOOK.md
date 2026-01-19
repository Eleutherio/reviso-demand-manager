# Runbook

Guia de troubleshooting para problemas comuns.

## Signup não funciona

### Sintoma
Cliente preenche formulário, mas não recebe email ou não é provisionado.

### Diagnóstico

1. **Verificar logs do backend**
```bash
docker compose logs backend | grep -i "signup\|stripe\|checkout"
```

2. **Verificar pending_signups**
```sql
SELECT * FROM pending_signups 
WHERE admin_email = 'email@cliente.com';
```

3. **Verificar Stripe Dashboard**
- Payments → Checkout Sessions
- Buscar por email ou agency_name

### Soluções

**Checkout Session não criado**:
- Verificar `STRIPE_API_KEY` configurado
- Verificar `stripe_price_id` nos planos:
```sql
SELECT code, stripe_price_id FROM subscription_plans;
```

**Webhook não recebido**:
- Verificar URL do webhook no Stripe Dashboard
- Testar com Stripe CLI:
```bash
stripe listen --forward-to localhost:8080/onboarding/webhook/stripe
stripe trigger checkout.session.completed
```

**Webhook recebido mas falhou**:
- Verificar `STRIPE_WEBHOOK_SECRET` configurado
- Verificar logs: `grep "webhook" logs/spring.log`

## Webhook duplicado

### Sintoma
Erro "Duplicate key value violates unique constraint" nos logs.

### Diagnóstico

```sql
SELECT event_id, created_at 
FROM stripe_webhook_events 
ORDER BY created_at DESC 
LIMIT 10;
```

### Solução

**Comportamento esperado**. Idempotência está funcionando.

Stripe envia eventos múltiplas vezes. Sistema retorna 200 e ignora duplicatas.

Nenhuma ação necessária.

## Agência não ativada

### Sintoma
Signup completo, pagamento aprovado, mas `agency.active = false`.

### Diagnóstico

1. **Verificar subscription**
```sql
SELECT s.status, s.current_period_start, s.current_period_end, a.active
FROM subscriptions s
JOIN agencies a ON s.agency_id = a.id
WHERE a.name = 'Nome da Agência';
```

2. **Verificar webhook events**
```sql
SELECT event_id, event_type, created_at
FROM stripe_webhook_events
WHERE event_type IN ('invoice.paid', 'customer.subscription.updated')
ORDER BY created_at DESC
LIMIT 10;
```

### Soluções

**Webhook `invoice.paid` não recebido**:
- Verificar configuração do webhook no Stripe
- Reenviar evento manualmente no Stripe Dashboard

**Subscription status incorreto**:
```sql
UPDATE subscriptions 
SET status = 'ACTIVE' 
WHERE stripe_subscription_id = 'sub_...';

UPDATE agencies 
SET active = true 
WHERE id = 'uuid';
```

## Login falha

### Sintoma
Credenciais corretas, mas retorna 401.

### Diagnóstico

1. **Verificar usuário existe**
```sql
SELECT id, email, role, active, company_id, agency_id
FROM users
WHERE email = 'email@usuario.com';
```

2. **Verificar senha**
```java
// No backend, adicionar log temporário em AuthController
log.info("Password match: {}", passwordEncoder.matches(dto.password(), user.getPasswordHash()));
```

### Soluções

**Usuário inativo**:
```sql
UPDATE users SET active = true WHERE email = 'email@usuario.com';
```

**Senha incorreta**:
```sql
-- Resetar senha (hash de "nova_senha")
UPDATE users 
SET password_hash = '$2a$10$N9qo8uLOickgx2ZMRZoMye.Jx3/kfRYPZCjQ3vZGZY.2K1zVvQmIy'
WHERE email = 'email@usuario.com';
```

**CLIENT_USER sem company_id**:
```sql
UPDATE users 
SET company_id = (SELECT id FROM companies WHERE company_code = 'CLIE-CL-ABC')
WHERE email = 'email@usuario.com';
```

**Usuário sem agency_id**:
```sql
UPDATE users 
SET agency_id = (SELECT agency_id FROM companies WHERE id = company_id)
WHERE email = 'email@usuario.com';
```

## Email não enviado

### Sintoma
Recovery de senha ou company code não chega.

### Diagnóstico

1. **Verificar email_outbox**
```sql
SELECT id, recipient, subject, status, attempts, last_error
FROM email_outbox
WHERE recipient = 'email@usuario.com'
ORDER BY created_at DESC;
```

2. **Verificar logs**
```bash
docker compose logs backend | grep -i "email\|resend"
```

### Soluções

**Status PENDING com attempts > 0**:
- Verificar `RESEND_API_KEY` configurado
- Verificar domínio verificado no Resend
- Verificar quota do Resend (100 emails/dia no free tier)

**Status QUOTA**:
- Aguardar 1 hora (retry automático)
- Ou aumentar plano do Resend

**Status FAILED**:
- Verificar `last_error` na tabela
- Reenviar manualmente:
```sql
UPDATE email_outbox 
SET status = 'PENDING', attempts = 0 
WHERE id = 'uuid';
```

## Rate limit atingido

### Sintoma
Erro 429 "Too Many Requests".

### Diagnóstico

```bash
docker compose logs backend | grep "Rate limit"
```

### Solução

**Comportamento esperado**. Aguardar 1 minuto.

Limites:
- Login: 10/min por IP
- Recovery: 5/min por IP + email
- Signup: 3/min por IP

**Ajustar limites** (se necessário):
```java
// RateLimitService.java
private static final int MAX_ATTEMPTS = 20; // Era 10
```

## Database connection pool esgotado

### Sintoma
Erro "Unable to acquire JDBC Connection".

### Diagnóstico

```bash
docker compose logs backend | grep -i "hikari\|connection"
```

### Solução

**Aumentar pool size**:
```properties
# application.properties
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
```

**Reiniciar aplicação**:
```bash
docker compose restart backend
```

## Flyway migration falha

### Sintoma
Aplicação não inicia, erro "Migration checksum mismatch".

### Diagnóstico

```sql
SELECT version, description, checksum, installed_on
FROM flyway_schema_history
ORDER BY installed_rank DESC;
```

### Solução

**Migration modificada após aplicação**:
```sql
-- Opção 1: Reparar (se migration não mudou estrutura)
DELETE FROM flyway_schema_history WHERE version = 'X';

-- Opção 2: Baseline (se banco já está correto)
-- Adicionar em application.properties:
spring.flyway.baseline-on-migrate=true
```

**Reiniciar aplicação**.

## Logs úteis

### Backend
```bash
# Todos os logs
docker compose logs backend -f

# Apenas erros
docker compose logs backend | grep -i "error\|exception"

# Webhooks
docker compose logs backend | grep -i "webhook"

# Auth
docker compose logs backend | grep -i "login\|jwt\|auth"
```

### Database
```bash
# Conectar
docker compose exec postgres psql -U reviso -d reviso

# Queries lentas
SELECT query, calls, total_time, mean_time
FROM pg_stat_statements
ORDER BY mean_time DESC
LIMIT 10;
```

### Stripe
- Dashboard → Developers → Events
- Dashboard → Developers → Webhooks → Logs

## Comandos úteis

### Resetar banco (DEV ONLY)
```bash
docker compose down -v
docker compose up -d
docker compose exec -T postgres psql -U reviso -d reviso < db/seed_data.sql
```

### Limpar pending_signups expirados
```sql
DELETE FROM pending_signups WHERE expires_at < now();
```

### Limpar email_outbox antigos
```sql
DELETE FROM email_outbox 
WHERE status IN ('SENT', 'FAILED') 
AND created_at < now() - interval '30 days';
```

### Verificar health
```bash
curl http://localhost:8080/actuator/health
```

## Contatos de Suporte

- Stripe: https://support.stripe.com
- Resend: https://resend.com/support
- PostgreSQL: https://www.postgresql.org/support/
