# Segurança

## Autenticação

**JWT** com HS384.

Claims:
- `sub`: userId
- `email`, `role`, `companyId`, `agencyId`
- `exp`: 24h (configurável)

Secret: Mínimo 256 bits via `JWT_SECRET`.

```bash
# Gerar secret
openssl rand -base64 64
```

## Senhas

**BCrypt** strength 10.

Regras:
- ✅ Hash antes de salvar
- ❌ Nunca plaintext no banco
- ❌ Nunca em logs
- ❌ Nunca em APIs externas (Stripe, Resend)

## Rate Limiting

| Endpoint | Limite |
|----------|--------|
| `/auth/login` | 10/min por IP |
| `/auth/login-client` | 10/min por IP |
| `/auth/recover-*` | 5/min por IP + email |
| `/onboarding/signup` | 3/min por IP |

Webhooks: **SEM rate limit** (usa idempotência).

## Webhook Stripe

### Validação

Usa SDK oficial (`stripe-java`):

```java
import com.stripe.net.Webhook;

Webhook.constructEvent(payload, signature, webhookSecret);
```

Valida:
- HMAC SHA256
- Timestamp (tolerância 5 min)
- Autenticidade do evento

### Idempotência

Constraint único no banco:

```sql
CREATE UNIQUE INDEX idx_webhook_event_id 
ON stripe_webhook_events(event_id);
```

Pattern insert-first:

```java
try {
    webhookEventRepository.save(new StripeWebhookEvent(eventId));
} catch (DataIntegrityViolationException e) {
    return ResponseEntity.ok().build(); // Já processado
}
// Processa evento
```

Garante processamento único mesmo com:
- Retries do Stripe
- Race conditions
- Duplicação de eventos

## CORS

Configurado em `SecurityConfig.java`:

```java
.allowedOrigins("http://localhost:4200") // Dev
.allowedOrigins("https://seu-dominio.com") // Prod
```

## Dados Sensíveis

**Nunca logar ou expor:**
- Senhas (plaintext ou hash)
- JWT secrets
- Stripe API keys
- Database credentials
- Resend API keys

**Usar variáveis de ambiente:**
```bash
JWT_SECRET=...
STRIPE_API_KEY=...
STRIPE_WEBHOOK_SECRET=...
RESEND_API_KEY=...
DB_PASSWORD=...
```

## LGPD

### Dados Pessoais Armazenados

- Nome completo
- Email
- Hash de senha (BCrypt)
- Stripe customer_id, subscription_id

### Dados NÃO Armazenados

- Senha plaintext
- Dados de pagamento (cartão)
- CPF/CNPJ (não coletado)

### Retenção

- `pending_signups`: 24h (cleanup automático)
- `agencies`, `users`, `companies`: Indefinido até exclusão manual
- `stripe_webhook_events`: Indefinido (auditoria)

### Direitos do Titular

Implementar manualmente:
- Acesso: `GET /admin/users/{id}`
- Retificação: `PATCH /admin/users/{id}`
- Exclusão: `DELETE /admin/users/{id}` (soft delete recomendado)

## Auditoria

### Request Events

Tabela `request_events` registra:
- Mudanças de status
- Atribuições
- Comentários
- Timestamp e userId

### Logs

Configurar em produção:
- Nível: INFO (não DEBUG)
- Rotation: Diário
- Retenção: 30 dias
- Excluir dados sensíveis

## Checklist Produção

- [ ] `JWT_SECRET` forte (64+ chars)
- [ ] `STRIPE_API_KEY` de produção (sk_live_)
- [ ] `STRIPE_WEBHOOK_SECRET` configurado
- [ ] CORS com domínio real
- [ ] HTTPS habilitado
- [ ] Rate limiting ativo
- [ ] Logs sem dados sensíveis
- [ ] Backup de banco configurado
- [ ] Monitoramento de erros (Sentry, etc)
