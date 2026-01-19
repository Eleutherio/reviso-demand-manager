# Onboarding SaaS

Sistema de cadastro self-service com pagamento via Stripe.

## Fluxo

```
1. Cliente acessa /onboarding/plans
2. Escolhe plano e preenche dados
3. POST /onboarding/signup → Stripe Checkout
4. Pagamento no Stripe
5. Webhook checkout.session.completed → Provisiona agência (inativa)
6. Webhook invoice.paid → Ativa agência
```

## Componentes

### 1. Planos (subscription_plans)

```sql
- id, code, name
- stripe_price_id, stripe_product_id
- max_users, max_requests_per_month
- active
```

Seed inicial:
- STARTER (5 usuários, 50 demandas/mês)
- PROFESSIONAL (20 usuários, 200 demandas/mês)
- ENTERPRISE (ilimitado)

### 2. Signup Temporário (pending_signups)

Armazena dados do signup até confirmação do pagamento:

```sql
- checkout_session_id (PK)
- plan_id, agency_name, admin_email
- password_hash (BCrypt, nunca plaintext)
- expires_at (24h)
```

**Segurança**: Senha é hasheada ANTES de criar Checkout Session. Nunca enviada ao Stripe.

### 3. Checkout Session

```java
POST /onboarding/signup
{
  "planId": "uuid",
  "agencyName": "Minha Agência",
  "adminEmail": "admin@agencia.com",
  "adminPassword": "senha_forte"
}
```

**Processo**:
1. Valida dados
2. Hash BCrypt da senha
3. Salva em `pending_signups`
4. Cria Checkout Session no Stripe (metadata: plan_id, agency_name, admin_email)
5. Retorna `sessionUrl`

### 4. Webhooks Stripe

#### checkout.session.completed
- Busca `pending_signup` por `checkout_session_id`
- Cria `agency` (active=false)
- Cria `company` (tipo AGENCY)
- Cria `user` AGENCY_ADMIN (com password_hash do pending_signup)
- Cria `subscription` (status=INCOMPLETE)
- Deleta `pending_signup`

#### invoice.paid
- Atualiza `subscription.status = ACTIVE`
- Atualiza `agency.active = true`

#### customer.subscription.updated
- Atualiza `subscription.status` e `subscription.current_period_*`
- Se status = PAST_DUE ou UNPAID: `agency.active = false`
- Se status = ACTIVE: `agency.active = true`

#### customer.subscription.deleted
- Atualiza `subscription.status = CANCELED`
- Atualiza `agency.active = false`

## Estados da Subscription

```
INCOMPLETE → ACTIVE (primeiro pagamento)
ACTIVE → PAST_DUE (falha no pagamento)
PAST_DUE → ACTIVE (pagamento recuperado)
PAST_DUE → UNPAID (retries esgotados)
UNPAID → CANCELED (cancelamento)
```

## Idempotência

Webhook events são processados uma única vez:

```sql
CREATE UNIQUE INDEX idx_webhook_event_id 
ON stripe_webhook_events(event_id);
```

Pattern insert-first:
```java
try {
    webhookEventRepository.save(new StripeWebhookEvent(eventId));
} catch (DataIntegrityViolationException e) {
    return 200; // Já processado
}
// Processa evento
```

## Configuração Stripe

### 1. Criar Produtos e Preços

Dashboard Stripe → Products:
- Starter: R$ 99/mês
- Professional: R$ 299/mês
- Enterprise: R$ 999/mês

Copiar `price_id` de cada plano.

### 2. Atualizar Banco

```sql
UPDATE subscription_plans 
SET stripe_price_id = 'price_...' 
WHERE code = 'STARTER';
```

### 3. Configurar Webhook

Dashboard Stripe → Webhooks → Add endpoint:
- URL: `https://seu-dominio.com/onboarding/webhook/stripe`
- Events: `checkout.session.completed`, `invoice.paid`, `customer.subscription.updated`, `customer.subscription.deleted`
- Copiar `Signing secret`

### 4. Variáveis de Ambiente

```bash
STRIPE_API_KEY=sk_live_...
STRIPE_WEBHOOK_SECRET=whsec_...
```

## Segurança

### Senha
- ✅ Hash BCrypt antes de salvar em `pending_signups`
- ✅ Nunca enviada ao Stripe (metadata)
- ✅ Nunca logada
- ✅ Expira em 24h

### Webhook
- ✅ Validação com Stripe SDK oficial
- ✅ Signature HMAC SHA256
- ✅ Timestamp tolerance (5 min)
- ✅ Idempotência (event_id único)

### Rate Limiting
- `/onboarding/signup`: 3 req/min por IP
- Webhook: SEM rate limit (usa idempotência)

## LGPD

Dados armazenados:
- Nome da agência
- Email do admin
- Hash de senha (BCrypt)
- Stripe customer_id, subscription_id

Dados NÃO armazenados:
- Senha plaintext
- Dados de pagamento (cartão)

Retenção:
- `pending_signups`: 24h (cleanup automático)
- `agencies`, `users`: indefinido (até exclusão manual)

## Troubleshooting

Ver [RUNBOOK.md](RUNBOOK.md).
